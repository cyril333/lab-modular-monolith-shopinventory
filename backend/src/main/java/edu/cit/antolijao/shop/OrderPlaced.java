package edu.cit.antolijao.shop;

public class OrderPlaced {
    private final Long orderId;

    public OrderPlaced(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() { return orderId; }
}