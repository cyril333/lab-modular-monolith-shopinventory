package edu.cit.antolijao.channel;

import edu.cit.antolijao.inventory.InventoryService;
import edu.cit.antolijao.supplier.SupplierOrderRepository;
import edu.cit.antolijao.supplier.SupplierOrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
class TiangeOutboxJob {
    @Autowired private TiangeOrderItemRepository orderItems;
    @Autowired private TiangeOrderRepository orders;
    @Autowired private TiangeClient client;
    @Autowired private TiangeStockSyncListener stockSync;
    @Autowired private TiangeOrderDecisionHandler decisionHandler;
    @Autowired private InventoryService inventoryService;
    @Autowired private SupplierOrderRepository supplierOrders;

    private long lastBackorderSweep = 0;

    @Scheduled(fixedDelay = 2000)
    void run() {
        try {
            for (TiangeOrder o : orders.findPendingSends()) {
                sendPending(o);
            }
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
        }
        try {
            stockSync.flushStock(() -> {
                Set<String> hold = new HashSet<>();
                for (TiangeOrder p : orders.findPendingSends()) {
                    // only an accepted order or a cancellation changes the stock Tiangge expects
                    if ("ACCEPTED".equals(p.getDecision()) || p.isCancelRequested()) {
                        orderItems.findByTiangeOrderId(p.getTiangeOrderId())
                                .forEach(i -> hold.add(i.getProductId()));
                    }
                }
                return hold;
            });
        } catch (Exception e) {
            System.out.println("Stock flush skipped: " + e.getMessage());
        }
    }

    private boolean sendPending(TiangeOrder o) {
        String id = o.getTiangeOrderId();
        try {
            if (!o.isDecisionSent()) {
                if (!readyToDecide(o)) {
                    return true; // checked again next cycle
                }
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
                stockSync.markDirty(orderItems.findByTiangeOrderId(id).stream()
                        .map(TiangeOrderItem::getProductId).toList());
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

    /** A backorder may only be announced once every short item has a purchase order LegacySupply really holds. */
    private boolean readyToDecide(TiangeOrder o) {
        if (!"BACKORDERED".equals(o.getDecision())) {
            return true;
        }
        List<TiangeOrderItem> lines = orderItems.findByTiangeOrderId(o.getTiangeOrderId());
        List<String> shortProducts = new ArrayList<>();
        for (TiangeOrderItem l : lines) {
            if (inventoryService.getItem(l.getProductId()).getStock() < l.getQty()) {
                shortProducts.add(l.getProductId());
            }
        }
        if (shortProducts.isEmpty()) {
            return false; // stock arrived meanwhile; the sweep accepts the order instead
        }
        List<SupplierOrderStatus> placed = List.of(
                SupplierOrderStatus.ACCEPTED, SupplierOrderStatus.PICKING, SupplierOrderStatus.SHIPPED);
        var held = supplierOrders.findByStatusIn(placed);
        boolean covered = shortProducts.stream()
                .allMatch(p -> held.stream().anyMatch(so -> so.getProductId().equals(p)));
        if (covered) {
            return true;
        }
        OffsetDateTime created = o.getCreatedAt();
        if (created != null && created.isBefore(OffsetDateTime.now().minusSeconds(40))) {
            o.setDecision("REJECTED"); // restock not placed in time: answer before the deadline
            orders.save(o);
            return true;
        }
        return false;
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