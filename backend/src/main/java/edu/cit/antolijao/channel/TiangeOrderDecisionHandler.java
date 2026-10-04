package edu.cit.antolijao.channel;

import edu.cit.antolijao.shop.OrderRequest;
import edu.cit.antolijao.shop.OrderResponse;
import edu.cit.antolijao.shop.OrderService;
import edu.cit.antolijao.supplier.SupplierGateway;
import edu.cit.antolijao.supplier.SupplierOrderRepository;
import edu.cit.antolijao.supplier.SupplierOrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
class TiangeOrderDecisionHandler {

    @Autowired
    private OrderService orderService;

    @Autowired
    private TiangeClient client;

    @Autowired
    private TiangeOrderRepository tiangeOrderRepository;

    @Autowired
    private TiangeOrderItemRepository tiangeOrderItemRepository;

    @Autowired
    private SupplierGateway supplierGateway;

    @Autowired
    private SupplierOrderRepository supplierOrderRepository;

    @Transactional
    void handleNewOrder(TiangeJson.FeedEvent event) {
        TiangeOrder record = new TiangeOrder(event.orderId);
        tiangeOrderRepository.save(record);

        for (TiangeJson.FeedLine line : event.lines) {
            tiangeOrderItemRepository.save(new TiangeOrderItem(event.orderId, line.sellerSku, line.qty));
        }

        OrderRequest request = new OrderRequest();
        List<OrderRequest.Item> items = new ArrayList<>();
        for (TiangeJson.FeedLine line : event.lines) {
            OrderRequest.Item item = new OrderRequest.Item();
            item.setProductId(line.sellerSku);
            item.setQuantity(line.qty);
            items.add(item);
        }
        request.setItems(items);

        OrderResponse response = orderService.placeOrder(request);

        if ("CONFIRMED".equals(response.getStatus())) {
            accept(event.orderId, record, findShopOrderId(response));
            return;
        }

        // REJECTED: check if every short item already has an open supplier order coming.
        if (canBackorder(event.lines)) {
            backorder(event.orderId, record);
        } else {
            reject(event.orderId, record, "Insufficient stock");
        }
    }

    private boolean canBackorder(List<TiangeJson.FeedLine> lines) {
        List<SupplierOrderStatus> openStatuses = List.of(
                SupplierOrderStatus.PENDING, SupplierOrderStatus.ACCEPTED,
                SupplierOrderStatus.PICKING, SupplierOrderStatus.SHIPPED
        );
        for (TiangeJson.FeedLine line : lines) {
            boolean hasOpenOrder = supplierOrderRepository.findByStatusIn(openStatuses).stream()
                    .anyMatch(so -> so.getProductId().equals(line.sellerSku));
            if (!hasOpenOrder) {
                // No restock coming for this product — trigger one now so we CAN backorder it.
                String buyerRef = "RO-TG-" + System.currentTimeMillis() + "-" + line.sellerSku;
                supplierGateway.placeReorder(line.sellerSku, line.qty, buyerRef);
            }
        }
        return true; // we've now ensured (or already had) an open supplier order for every line
    }

    private void accept(String orderId, TiangeOrder record, String shopOrderId) {
        record.setShopOrderId(shopOrderId == null ? null : Long.valueOf(shopOrderId));
        record.setDecision("ACCEPTED");
        tiangeOrderRepository.save(record);

        TiangeJson.DecisionRequest req = new TiangeJson.DecisionRequest("ACCEPTED", shopOrderId, null);
        client.decide(orderId, req);
    }

    private void reject(String orderId, TiangeOrder record, String reason) {
        record.setDecision("REJECTED");
        tiangeOrderRepository.save(record);

        TiangeJson.DecisionRequest req = new TiangeJson.DecisionRequest("REJECTED", null, reason);
        client.decide(orderId, req);
    }

    private void backorder(String orderId, TiangeOrder record) {
        record.setDecision("BACKORDERED");
        tiangeOrderRepository.save(record);

        TiangeJson.DecisionRequest req = new TiangeJson.DecisionRequest("BACKORDERED", null, "Awaiting supplier delivery");
        client.decide(orderId, req);
    }

    private String findShopOrderId(OrderResponse response) {
    return response.getOrderId() == null ? null : String.valueOf(response.getOrderId());
    }
 

    void handleCancellation(TiangeJson.FeedEvent event) {
        Optional<TiangeOrder> record = tiangeOrderRepository.findById(event.orderId);
        if (record.isEmpty() || record.get().getShopOrderId() == null) {
            return;
        }
        if (record.get().isCancelConfirmed()) {
            // Already confirmed — safe to re-confirm per manual, but skip local work.
            client.confirmCancellation(event.orderId, new TiangeJson.CancellationConfirmRequest(true));
            return;
        }

        orderService.cancelOrder(record.get().getShopOrderId());

        record.get().setCancelConfirmed(true);
        tiangeOrderRepository.save(record.get());

        client.confirmCancellation(event.orderId, new TiangeJson.CancellationConfirmRequest(true));
    }
}