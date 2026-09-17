package edu.cit.antolijao.shop;

public class OrderRejected {
    private final Long orderId;
    private final String reason;

    public OrderRejected(Long orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }

    public Long getOrderId() { return orderId; }
    public String getReason() { return reason; }
}