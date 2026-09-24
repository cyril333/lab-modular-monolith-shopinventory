package edu.cit.antolijao.supplier;

import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SupplierOrderPoller {

    private final SupplierOrderRepository repository;
    private final LegacySupplyClient client;
    private final ApplicationEventPublisher publisher;

    SupplierOrderPoller(SupplierOrderRepository repository,
                         LegacySupplyClient client,
                         ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.client = client;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelay = 45000) // every 45s, stays under quota
    void pollOpenOrders() {
        List<SupplierOrder> open = repository.findByStatusIn(
                List.of(SupplierOrderStatus.ACCEPTED,
                        SupplierOrderStatus.PICKING,
                        SupplierOrderStatus.SHIPPED));

        for (SupplierOrder order : open) {
            if (order.getPoNumber() == null) continue; // never got a PO number, skip
            try {
                LegacySupplyXml.PurchaseOrderStatusXml statusXml = client.getOrderStatus(order.getPoNumber());
                SupplierOrderStatus mapped = mapStatusCode(statusXml.statusCode);
                order.setStatus(mapped);
                repository.save(order);

                if (mapped == SupplierOrderStatus.DELIVERED) {
                    publisher.publishEvent(new SupplierOrderDelivered(
                            order.getProductId(), order.getUnits(), order.getPoNumber()));
                }
            } catch (LegacySupplyException e) {
            }
        }
    }

    private SupplierOrderStatus mapStatusCode(int code) {
        return switch (code) {
            case 10 -> SupplierOrderStatus.ACCEPTED;
            case 20 -> SupplierOrderStatus.PICKING;
            case 30 -> SupplierOrderStatus.SHIPPED;
            case 40 -> SupplierOrderStatus.DELIVERED;
            default -> SupplierOrderStatus.ACCEPTED; // unknown code: log + leave unchanged, document in INTEGRATION.md
        };
    }
}