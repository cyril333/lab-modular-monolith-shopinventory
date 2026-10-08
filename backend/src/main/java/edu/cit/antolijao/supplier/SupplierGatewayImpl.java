package edu.cit.antolijao.supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Optional;
import java.util.UUID;

@Service
class SupplierGatewayImpl implements SupplierGateway {

    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private LegacySupplyClient client;

    @Autowired
    private SupplierOrderRepository supplierOrderRepository;

    @Autowired
    private ProductSupplierMapping productSupplierMapping;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SupplierOrderResult placeReorder(String productId, int unitsNeeded, String buyerRef) {
        String requestId = UUID.randomUUID().toString();

        // Idempotency guard: if caller supplied a buyerRef that already exists, return early[cite: 1].
        if (buyerRef != null && !buyerRef.isBlank()) {
            Optional<SupplierOrder> existing = supplierOrderRepository.findByBuyerRef(buyerRef);
            if (existing.isPresent()) {
                return SupplierOrderResult.pending("Reorder already recorded for " + buyerRef);
            }
        }

        ProductSupplierMapping.Mapping mapping = productSupplierMapping.forProduct(productId);
        int cases = (int) Math.ceil((double) unitsNeeded / mapping.packSize());

        // buyer_ref is NOT NULL + UNIQUE, so the first insert needs a unique temporary value
        SupplierOrder order = new SupplierOrder(productId, "TMP-" + requestId, requestId, unitsNeeded, SupplierOrderStatus.PENDING);
        order.setCases(cases);
        order = supplierOrderRepository.save(order);

        if (buyerRef == null || buyerRef.isBlank()) {
            buyerRef = "RO-L4-" + order.getId();
        }

        order.setBuyerRef(buyerRef);
        order = supplierOrderRepository.save(order);

        return attemptSend(order, mapping, cases);
    }

    SupplierOrderResult attemptSend(SupplierOrder order, ProductSupplierMapping.Mapping mapping, int cases) {
        int attempt = 0;
        long backoffMillis = 500;

        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try {
                LegacySupplyXml.PurchaseOrderAck ack = client.placeOrder(
                        mapping.supplierSku(), cases, order.getBuyerRef(), order.getRequestId()
                );

                order.setPoNumber(ack.poNumber);
                order.setStatus(SupplierOrderStatus.ACCEPTED);
                supplierOrderRepository.save(order);

                return SupplierOrderResult.accepted(ack.poNumber);

            } catch (LegacySupplyException e) {
                System.out.println("LegacySupply call failed: " + e.errorCode + " (HTTP " + e.httpStatus + ")");
                boolean retryable = e.httpStatus == 503 || e.httpStatus == 429;
                if (!retryable || attempt >= MAX_ATTEMPTS) {
                    return SupplierOrderResult.pending(
                            "LegacySupply unavailable (" + e.errorCode + "), queued for retry"
                    );
                }
                sleepQuietly(backoffMillis);
                backoffMillis *= 2;
            }
        }

        return SupplierOrderResult.pending("Exhausted retries, queued for later");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}