package edu.cit.antolijao.channel;

import edu.cit.antolijao.shop.OrderRequest;
import edu.cit.antolijao.shop.OrderResponse;
import edu.cit.antolijao.shop.OrderService;
import edu.cit.antolijao.supplier.SupplierOrderDelivered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
class TiangeBackorderResolver {

    @Autowired
    private TiangeOrderRepository tiangeOrderRepository;

    @Autowired
    private TiangeOrderItemRepository tiangeOrderItemRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TiangeClient client;

    @EventListener
    @Transactional
    void onSupplierOrderDelivered(SupplierOrderDelivered event) {
        try {
            resolveBackordersFor(event.getProductId());
        } catch (Exception e) {
            System.out.println("Backorder resolution failed for " + event.getProductId() + ": " + e.getMessage());
        }
    }

    private void resolveBackordersFor(String productId) {
        List<TiangeOrderItem> affectedItems = tiangeOrderItemRepository.findByProductId(productId);

        for (TiangeOrderItem item : affectedItems) {
            tiangeOrderRepository.findById(item.getTiangeOrderId()).ifPresent(tgOrder -> {
                if (!"BACKORDERED".equals(tgOrder.getDecision())) {
                    return; // already resolved or never was a backorder
                }
                attemptResolve(tgOrder);
            });
        }
    }

    private void attemptResolve(TiangeOrder tgOrder) {
        List<TiangeOrderItem> items = tiangeOrderItemRepository.findByTiangeOrderId(tgOrder.getTiangeOrderId());

        OrderRequest request = new OrderRequest();
        List<OrderRequest.Item> reqItems = new ArrayList<>();
        for (TiangeOrderItem item : items) {
            OrderRequest.Item ri = new OrderRequest.Item();
            ri.setProductId(item.getProductId());
            ri.setQuantity(item.getQty());
            reqItems.add(ri);
        }
        request.setItems(reqItems);

        OrderResponse response = orderService.placeOrder(request);

        if ("CONFIRMED".equals(response.getStatus())) {
            tgOrder.setShopOrderId(response.getOrderId());
            tgOrder.setDecision("ACCEPTED");
            tiangeOrderRepository.save(tgOrder);

            client.resolve(tgOrder.getTiangeOrderId(), new TiangeJson.ResolutionRequest("ACCEPTED"));
        }
        // If still REJECTED, leave as BACKORDERED — will retry on the next delivery for this product.
    }
}