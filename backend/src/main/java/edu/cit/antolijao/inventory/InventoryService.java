package edu.cit.antolijao.inventory;

public interface InventoryService {

    Inventory getItem(String productId);

    /**
     * Attempts to reserve (deduct) stock for an order.
     * Returns true if successful, false if there wasn't enough stock.
     */
    boolean reserve(String productId, int quantity);

    /**
     * Returns previously reserved stock back to inventory (used on order cancellation).
     */
    void restock(String productId, int quantity);
}