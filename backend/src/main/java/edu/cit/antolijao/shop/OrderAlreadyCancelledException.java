package edu.cit.antolijao.shop;

public class OrderAlreadyCancelledException extends RuntimeException {
    public OrderAlreadyCancelledException(Long orderId) {
        super("Order already cancelled: " + orderId);
    }
}