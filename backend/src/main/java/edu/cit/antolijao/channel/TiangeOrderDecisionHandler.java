package edu.cit.antolijao.channel;

import edu.cit.antolijao.inventory.InventoryService;
import edu.cit.antolijao.shop.OrderRequest;
import edu.cit.antolijao.shop.OrderResponse;
import edu.cit.antolijao.shop.OrderService;
import edu.cit.antolijao.supplier.SupplierGateway;
import edu.cit.antolijao.supplier.SupplierOrderRepository;
import edu.cit.antolijao.supplier.SupplierOrderResult;
import edu.cit.antolijao.supplier.SupplierOrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
class TiangeOrderDecisionHandler {

    // Serializes everything that reserves or restocks stock, so two threads can never oversell.
    static final Object LOCK = new Object();

    private static final List<SupplierOrderStatus> OPEN = List.of(
            SupplierOrderStatus.PENDING, SupplierOrderStatus.ACCEPTED,
            SupplierOrderStatus.PICKING, SupplierOrderStatus.SHIPPED);

    @Autowired private OrderService orderService;
    @Autowired private InventoryService inventoryService;
    @Autowired private TiangeOrderRepository tiangeOrderRepository;
    @Autowired private TiangeOrderItemRepository tiangeOrderItemRepository;
    @Autowired private SupplierGateway supplierGateway;
    @Autowired private SupplierOrderRepository supplierOrderRepository;

    /** Local work only. Tiangge hears about the decision later, from the outbox. */
    @Transactional
    void handleNewOrder(TiangeJson.FeedEvent event) {
        if (tiangeOrderRepository.existsById(event.orderId)) {
            return; // redelivery
        }

        TiangeOrder record = new TiangeOrder(event.orderId);
        tiangeOrderRepository.save(record);

        List<OrderRequest.Item> items = new ArrayList<>();
        for (TiangeJson.FeedLine line : event.lines) {
            tiangeOrderItemRepository.save(new TiangeOrderItem(event.orderId, line.sellerSku, line.qty));
            OrderRequest.Item item = new OrderRequest.Item();
            item.setProductId(line.sellerSku);
            item.setQuantity(line.qty);
            items.add(item);
        }
        OrderRequest request = new OrderRequest();
        request.setItems(items);

        OrderResponse response = orderService.placeOrder(request);
        record.setShopOrderId(response.getOrderId());

        if ("CONFIRMED".equals(response.getStatus())) {
            record.setDecision("ACCEPTED");
        } else if (canBackorder(event.orderId, event.lines)) {
            record.setDecision("BACKORDERED");
        } else {
            record.setDecision("REJECTED");
        }
        tiangeOrderRepository.save(record);
    }

    @Transactional
    void handleCancellation(TiangeJson.FeedEvent event) {
        TiangeOrder record = tiangeOrderRepository.findById(event.orderId).orElse(null);

        if (record == null) { // cancellation for an order we never saw: just confirm it
            record = new TiangeOrder(event.orderId);
            record.setDecision("CANCELLED");
            record.setDecisionSent(true);
            record.setCancelRequested(true);
            tiangeOrderRepository.save(record);
            return;
        }
        if (record.isCancelRequested()) {
            return;
        }
        if ("ACCEPTED".equals(record.getDecision()) && record.getShopOrderId() != null) {
            orderService.cancelOrder(record.getShopOrderId()); // restocks inventory
        }
        record.setCancelRequested(true);
        tiangeOrderRepository.save(record);
    }

    /** Called when a delivery arrives (allowCancel = true) and by the outbox sweep (allowCancel = false). */
    @Transactional
    void tryResolveBackorder(String tiangeOrderId, boolean allowCancel) {
        TiangeOrder o = tiangeOrderRepository.findById(tiangeOrderId).orElse(null);
        if (o == null || !"BACKORDERED".equals(o.getDecision()) || o.isCancelRequested()) {
            return;
        }

        List<TiangeOrderItem> lines = tiangeOrderItemRepository.findByTiangeOrderId(tiangeOrderId);
        List<String> shortProducts = new ArrayList<>();
        for (TiangeOrderItem l : lines) {
            if (inventoryService.getItem(l.getProductId()).getStock() < l.getQty()) {
                shortProducts.add(l.getProductId());
            }
        }

        if (shortProducts.isEmpty()) {
            List<OrderRequest.Item> items = new ArrayList<>();
            for (TiangeOrderItem l : lines) {
                OrderRequest.Item item = new OrderRequest.Item();
                item.setProductId(l.getProductId());
                item.setQuantity(l.getQty());
                items.add(item);
            }
            OrderRequest request = new OrderRequest();
            request.setItems(items);

            OrderResponse response = orderService.placeOrder(request);
            if ("CONFIRMED".equals(response.getStatus())) {
                o.setShopOrderId(response.getOrderId());
                o.setDecision("ACCEPTED");
                if (o.isDecisionSent()) {
                    o.setResolution("ACCEPTED");
                }
                tiangeOrderRepository.save(o);
            }
            return;
        }

        boolean stillComing = shortProducts.stream().anyMatch(this::hasOpenSupplierOrder);
        if (!stillComing && allowCancel) {
            o.setDecision("CANCELLED");
            if (o.isDecisionSent()) {
                o.setResolution("CANCELLED");
            }
            tiangeOrderRepository.save(o);
        }
    }

    private boolean canBackorder(String tiangeOrderId, List<TiangeJson.FeedLine> lines) {
        boolean anyShort = false;
        for (TiangeJson.FeedLine line : lines) {
            int stock = inventoryService.getItem(line.sellerSku).getStock();
            if (stock >= line.qty) {
                continue;
            }
            anyShort = true;
            if (hasOpenSupplierOrder(line.sellerSku)) {
                continue;
            }
            try {
                String buyerRef = "RO-" + tiangeOrderId + "-" + line.sellerSku;
                supplierGateway.queueReorder(line.sellerSku, Math.max(line.qty - stock, 20), buyerRef);
            } catch (Exception e) {
                return false;
            }
        }
        return anyShort;
    }

    private boolean hasOpenSupplierOrder(String productId) {
        return supplierOrderRepository.findByStatusIn(OPEN).stream()
                .anyMatch(so -> so.getProductId().equals(productId));
    }
}