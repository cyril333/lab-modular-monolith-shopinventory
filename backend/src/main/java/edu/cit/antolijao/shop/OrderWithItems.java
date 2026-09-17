package edu.cit.antolijao.shop;

import java.time.OffsetDateTime;
import java.util.List;

public class OrderWithItems {
    private Long orderId;
    private String status;
    private String reason;
    private OffsetDateTime createdAt;
    private List<OrderItem> items;

    public OrderWithItems(Long orderId, String status, String reason, OffsetDateTime createdAt, List<OrderItem> items) {
        this.orderId = orderId;
        this.status = status;
        this.reason = reason;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getOrderId() { return orderId; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}