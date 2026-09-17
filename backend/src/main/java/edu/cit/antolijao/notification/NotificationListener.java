package edu.cit.antolijao.notification;

import edu.cit.antolijao.inventory.LowStock;
import edu.cit.antolijao.shop.OrderPlaced;
import edu.cit.antolijao.shop.OrderRejected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @Autowired
    private NotificationRepository notificationRepository;

    @EventListener
    public void onOrderPlaced(OrderPlaced event) {
        String message = "Order " + event.getOrderId() + " confirmed";
        notificationRepository.save(new Notification(message));
    }

    @EventListener
    public void onOrderRejected(OrderRejected event) {
        String message = "Order " + event.getOrderId() + " rejected";
        notificationRepository.save(new Notification(message));
    }

    @EventListener
    public void onLowStock(LowStock event) {
        String message = "Reorder needed: " + event.getProductId() +
                " is low on stock (" + event.getRemainingStock() + " remaining)";
        notificationRepository.save(new Notification(message));
    }
}