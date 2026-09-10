package edu.cit.antolijao.inventory;

public interface InventoryService {

    Inventory getItem(String productId);

    boolean reserve(String productId, int quantity);
}