package edu.cit.antolijao.shop;

import edu.cit.antolijao.inventory.InventoryService;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final InventoryService inventoryService;
    private final OrderRepository orderRepository;

    public OrderService(InventoryService inventoryService, OrderRepository orderRepository) {
        this.inventoryService = inventoryService;
        this.orderRepository = orderRepository;
    }

    public Order placeOrder(String productId, int quantity) {
        boolean reserved = inventoryService.reserve(productId, quantity);

        Order order;
        if (reserved) {
            order = new Order(productId, quantity, "CONFIRMED", null);
        } else {
            order = new Order(productId, quantity, "REJECTED", "Insufficient stock");
        }

        return orderRepository.save(order);
    }

    public Integer getRemainingStock(String productId) {
        return inventoryService.getItem(productId).getStock();
    }
}