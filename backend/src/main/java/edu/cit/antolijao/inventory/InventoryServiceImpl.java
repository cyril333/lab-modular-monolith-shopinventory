package edu.cit.antolijao.inventory;

import edu.cit.antolijao.supplier.SupplierGateway;
import edu.cit.antolijao.supplier.SupplierOrderResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class InventoryServiceImpl implements InventoryService {

    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int REORDER_QUANTITY = 20; // units to request per auto-reorder

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private SupplierGateway supplierGateway;

    @Autowired
    private edu.cit.antolijao.supplier.SupplierOrderRepository supplierOrderRepository;

    @Override
    public Inventory getItem(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown product: " + productId));
    }

    @Override
    @Transactional
    public boolean reserve(String productId, int quantity) {
        Optional<Inventory> found = inventoryRepository.findByProductId(productId);
        if (found.isEmpty()) {
            return false;
        }

        Inventory item = found.get();
        if (item.getStock() < quantity) {
            return false;
        }

        item.setStock(item.getStock() - quantity);
        inventoryRepository.save(item);

        if (item.getStock() < LOW_STOCK_THRESHOLD) {
            eventPublisher.publishEvent(new LowStock(productId, item.getStock()));
            triggerAutoReorder(productId);
        }

        return true;
    }

    @Override
    @Transactional
    public void restock(String productId, int quantity) {
        inventoryRepository.findByProductId(productId).ifPresent(item -> {
            item.setStock(item.getStock() + quantity);
            inventoryRepository.save(item);
        });
    }

    private void triggerAutoReorder(String productId) {
        long sequence = supplierOrderRepository.count() + 1;
        String buyerRef = "RO-" + sequence;
        SupplierOrderResult result = supplierGateway.placeReorder(productId, REORDER_QUANTITY, buyerRef);
        // Result is intentionally not surfaced here — SupplierOrder table + notifications
        // (added in Part E) are the system of record for what happened.
    }
}