package edu.cit.antolijao.supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
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

        // Save initial PENDING entity first to obtain the auto-generated DB primary key (ID)
        SupplierOrder order = new SupplierOrder(productId, null, requestId, unitsNeeded, SupplierOrderStatus.PENDING);
        order.setCases(cases);
        order = supplierOrderRepository.save(order);

        // Derive deterministic buyerRef from saved primary key if none was provided by caller
        if (buyerRef == null || buyerRef.isBlank()) {
            buyerRef = String.format("RO-TEST-%03d", order.getId());
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