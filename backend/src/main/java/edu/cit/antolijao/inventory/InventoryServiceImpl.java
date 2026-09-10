package edu.cit.antolijao.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    InventoryServiceImpl(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

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
        return true;
    }
}