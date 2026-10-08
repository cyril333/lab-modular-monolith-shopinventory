package edu.cit.antolijao.channel;

import edu.cit.antolijao.supplier.SupplierOrderDelivered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
class TiangeBackorderResolver {

    @Autowired private TiangeOrderItemRepository tiangeOrderItemRepository;
    @Autowired private TiangeOrderDecisionHandler decisionHandler;

    @EventListener
    void onSupplierOrderDelivered(SupplierOrderDelivered event) {
        Set<String> seen = new HashSet<>();
        for (TiangeOrderItem item : tiangeOrderItemRepository.findByProductId(event.getProductId())) {
            if (!seen.add(item.getTiangeOrderId())) {
                continue;
            }
            try {
                synchronized (TiangeOrderDecisionHandler.LOCK) {
                    decisionHandler.tryResolveBackorder(item.getTiangeOrderId(), true);
                }
            } catch (Exception e) {
                System.out.println("Backorder resolution failed for " + item.getTiangeOrderId() + ": " + e.getMessage());
            }
        }
    }
}