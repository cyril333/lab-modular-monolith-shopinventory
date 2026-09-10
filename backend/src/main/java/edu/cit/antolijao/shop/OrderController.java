package edu.cit.antolijao.shop;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        Order order = orderService.placeOrder(request.getProductId(), request.getQuantity());

        Integer remainingStock = orderService.getRemainingStock(request.getProductId());

        OrderResponse response = new OrderResponse(
                order.getStatus(),
                order.getReason(),
                remainingStock
        );

        return ResponseEntity.ok(response);
    }
}