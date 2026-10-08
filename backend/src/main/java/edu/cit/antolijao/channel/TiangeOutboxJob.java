package edu.cit.antolijao.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class TiangeOutboxJob {

    @Autowired private TiangeOrderRepository orders;
    @Autowired private TiangeClient client;
    @Autowired private TiangeStockSyncListener stockSync;
    @Autowired private TiangeOrderDecisionHandler decisionHandler;

    private long lastBackorderSweep = 0;

    @Scheduled(fixedDelay = 2000)
    void run() {
        boolean allSent = true;
        try {
            // 1. deadline-critical: tell Tiangge every decision first
            for (TiangeOrder o : orders.findPendingSends()) {
                if (!sendPending(o)) {
                    allSent = false;
                }
            }
            // 2. backorder sweep, at most every 10s (deliveries also resolve backorders via the resolver)
            long now = System.currentTimeMillis();
            if (now - lastBackorderSweep >= 10_000) {
                lastBackorderSweep = now;
                for (TiangeOrder o : orders.findByDecision("BACKORDERED")) {
                    try {
                        synchronized (TiangeOrderDecisionHandler.LOCK) {
                            decisionHandler.tryResolveBackorder(o.getTiangeOrderId(), false);
                        }
                    } catch (Exception e) {
                        System.out.println("Backorder check failed for " + o.getTiangeOrderId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Outbox run failed: " + e.getMessage());
            allSent = false;
        }
        // 3. stock goes out only if Tiangge has every decision, checked again right before sending
        if (allSent && orders.findPendingSends().isEmpty()) {
            stockSync.flushStock();
        }
    }

    private boolean sendPending(TiangeOrder o) {
        String id = o.getTiangeOrderId();
        try {
            if (!o.isDecisionSent()) {
                sendDecision(o);
                orders.markDecisionSent(id);
            }
            if (o.getResolution() != null && !o.isResolutionSent()) {
                client.resolve(id, new TiangeJson.ResolutionRequest(o.getResolution()));
                orders.markResolutionSent(id);
            }
            if (o.isCancelRequested() && !o.isCancelConfirmed()) {
                boolean restocked = o.getShopOrderId() != null && "ACCEPTED".equals(o.getDecision());
                client.confirmCancellation(id, new TiangeJson.CancellationConfirmRequest(restocked));
                orders.markCancelConfirmed(id);
            }
            return true;
        } catch (TiangeException e) {
            if (isPermanent(e)) {
                System.out.println("Giving up on " + id + ": " + e.getMessage());
                orders.markAllSent(id);
                return true;
            }
            System.out.println("Will retry " + id + ": " + e.getMessage());
            return false;
        } catch (Exception e) { // timeouts etc.: outcome unknown, resending is safe
            System.out.println("Will retry " + id + ": " + e.getMessage());
            return false;
        }
    }

    private void sendDecision(TiangeOrder o) {
        String decision = "CANCELLED".equals(o.getDecision()) ? "REJECTED" : o.getDecision();
        String shopId = o.getShopOrderId() == null ? null : String.valueOf(o.getShopOrderId());
        String reason = "REJECTED".equals(decision) ? "Insufficient stock"
                : "BACKORDERED".equals(decision) ? "Awaiting supplier delivery" : null;
        try {
            client.decide(o.getTiangeOrderId(), new TiangeJson.DecisionRequest(decision, shopId, reason));
        } catch (TiangeException e) {
            if (e.errorCode == null || !e.errorCode.startsWith("decision_conflict")) {
                throw e;
            }
            // Tiangge already holds a decision from an earlier attempt whose reply we never saw.
            Matcher m = Pattern.compile("already decided as (\\w+)").matcher(e.errorCode);
            String existing = m.find() ? m.group(1) : "";
            if ("BACKORDERED".equals(existing) && !"BACKORDERED".equals(decision)) {
                client.resolve(o.getTiangeOrderId(),
                        new TiangeJson.ResolutionRequest("ACCEPTED".equals(decision) ? "ACCEPTED" : "CANCELLED"));
            }
        }
    }

    private boolean isPermanent(TiangeException e) {
        return e.httpStatus >= 400 && e.httpStatus < 500 && e.httpStatus != 408 && e.httpStatus != 429;
    }
}