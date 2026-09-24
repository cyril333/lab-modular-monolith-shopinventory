package edu.cit.antolijao.inventory;

import edu.cit.antolijao.supplier.SupplierOrderDelivered;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class SupplierOrderDeliveredListener {

    private final InventoryService inventoryService;

    SupplierOrderDeliveredListener(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @EventListener
    void onDelivered(SupplierOrderDelivered event) {
        inventoryService.restock(event.getProductId(), event.getUnits());
    }
}