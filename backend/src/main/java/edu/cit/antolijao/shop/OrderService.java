package edu.cit.antolijao.shop;

import edu.cit.antolijao.inventory.Inventory;
import edu.cit.antolijao.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderService(
            InventoryService inventoryService,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
 
        for (OrderRequest.Item item : request.getItems()) {
            Inventory current = inventoryService.getItem(item.getProductId());
            if (current.getStock() < item.getQuantity()) {
                Order rejectedOrder = new Order("REJECTED", "Insufficient stock");
                orderRepository.save(rejectedOrder);

                eventPublisher.publishEvent(new OrderRejected(rejectedOrder.getOrderId(), rejectedOrder.getReason()));

                String failingProductId = item.getProductId();
                List<OrderResponse.ItemOutcome> outcomes = new ArrayList<>();
                for (OrderRequest.Item i : request.getItems()) {
                    String name = inventoryService.getItem(i.getProductId()).getName();
                    String outcome = i.getProductId().equals(failingProductId) ? "INSUFFICIENT_STOCK" : "NOT_RESERVED";
                    outcomes.add(new OrderResponse.ItemOutcome(i.getProductId(), name, outcome));
                }
                return new OrderResponse("REJECTED", rejectedOrder.getReason(), outcomes);
            }
        }

        Order confirmedOrder = new Order("CONFIRMED", null);
        orderRepository.save(confirmedOrder);

        List<OrderResponse.ItemOutcome> outcomes = new ArrayList<>();
        for (OrderRequest.Item item : request.getItems()) {
            inventoryService.reserve(item.getProductId(), item.getQuantity());
            orderItemRepository.save(new OrderItem(confirmedOrder.getOrderId(), item.getProductId(), item.getQuantity()));
            String name = inventoryService.getItem(item.getProductId()).getName();
            outcomes.add(new OrderResponse.ItemOutcome(item.getProductId(), name, "RESERVED"));
        }

        eventPublisher.publishEvent(new OrderPlaced(confirmedOrder.getOrderId()));

        return new OrderResponse("CONFIRMED", null, outcomes);
    }

    @Transactional
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if ("CANCELLED".equals(order.getStatus())) {
            throw new OrderAlreadyCancelledException(orderId);
        }

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            inventoryService.restock(item.getProductId(), item.getQuantity());
        }

        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }
}