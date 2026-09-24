package edu.cit.antolijao.supplier;

import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class SupplierOrderRetryJob {

    private final SupplierOrderRepository repository;
    private final SupplierGatewayImpl gateway;
    private final ProductSupplierMapping productSupplierMapping;

    SupplierOrderRetryJob(SupplierOrderRepository repository,
                           SupplierGatewayImpl gateway,
                           ProductSupplierMapping productSupplierMapping) {
        this.repository = repository;
        this.gateway = gateway;
        this.productSupplierMapping = productSupplierMapping;
    }

    @Scheduled(fixedDelay = 60000) // every 60s, separate cadence from status poller
    void retryPendingOrders() {
        List<SupplierOrder> pending = repository.findByStatus(SupplierOrderStatus.PENDING);

        for (SupplierOrder order : pending) {
            ProductSupplierMapping.Mapping mapping = productSupplierMapping.forProduct(order.getProductId());
            // Reuses order.getRequestId() / order.getBuyerRef() already stored on the entity —
            // never regenerates them, guaranteeing no duplicate orders across restarts.
            gateway.attemptSend(order, mapping, order.getCases());
        }
    }
}