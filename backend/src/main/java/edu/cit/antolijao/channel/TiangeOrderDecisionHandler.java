package edu.cit.antolijao.channel;

import edu.cit.antolijao.shop.OrderRequest;
import edu.cit.antolijao.shop.OrderResponse;
import edu.cit.antolijao.shop.OrderService;
import edu.cit.antolijao.supplier.SupplierGateway;
import edu.cit.antolijao.supplier.SupplierOrderRepository;
import edu.cit.antolijao.supplier.SupplierOrderStatus;
import edu.cit.antolijao.inventory.InventoryService;
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

    @Autowired
    private InventoryService inventoryService;

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
        String shopOrderId = findShopOrderId(response);

        if ("CONFIRMED".equals(response.getStatus())) {
            accept(event.orderId, record, shopOrderId);
            return;
        }

        if (canBackorder(event.orderId, event.lines)) {
            backorder(event.orderId, record, shopOrderId);
        } else {
            reject(event.orderId, record, shopOrderId, "Insufficient stock");
        }
    }

    private boolean canBackorder(String tiangeOrderId, List<TiangeJson.FeedLine> lines) {
    List<SupplierOrderStatus> open = List.of(
            SupplierOrderStatus.PENDING, SupplierOrderStatus.ACCEPTED,
            SupplierOrderStatus.PICKING, SupplierOrderStatus.SHIPPED);

    for (TiangeJson.FeedLine line : lines) {
        int stock = inventoryService.getItem(line.sellerSku).getStock();
        if (stock >= line.qty) {
            continue; // not short on this line
        }
        boolean hasOpenOrder = supplierOrderRepository.findByStatusIn(open).stream()
                .anyMatch(so -> so.getProductId().equals(line.sellerSku));
        if (!hasOpenOrder) {
            try {
                String buyerRef = "RO-" + tiangeOrderId + "-" + line.sellerSku;
                supplierGateway.placeReorder(line.sellerSku, Math.max(line.qty - stock, 20), buyerRef);
            } catch (Exception e) {
                return false; // can't restock this product, so reject
            }
        }
    }
    return true;
}

    private void reject(String orderId, TiangeOrder record, String shopOrderId, String reason) {
        record.setDecision("REJECTED");
        tiangeOrderRepository.save(record);
        client.decide(orderId, new TiangeJson.DecisionRequest("REJECTED", shopOrderId, reason));
    }

    private void backorder(String orderId, TiangeOrder record, String shopOrderId) {
        record.setDecision("BACKORDERED");
        tiangeOrderRepository.save(record);
        client.decide(orderId, new TiangeJson.DecisionRequest("BACKORDERED", shopOrderId, "Awaiting supplier delivery"));
    }

    private void accept(String orderId, TiangeOrder record, String shopOrderId) {
    record.setShopOrderId(shopOrderId == null ? null : Long.valueOf(shopOrderId));
    record.setDecision("ACCEPTED");
    tiangeOrderRepository.save(record);

    client.decide(orderId, new TiangeJson.DecisionRequest("ACCEPTED", shopOrderId, null));
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