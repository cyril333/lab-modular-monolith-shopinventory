package edu.cit.antolijao.channel;

import edu.cit.antolijao.inventory.InventoryService;
import edu.cit.antolijao.inventory.StockChanged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
class TiangeStockSyncListener {

    @Autowired private TiangeClient client;
    @Autowired private InventoryService inventoryService;

    private final Set<String> dirty = ConcurrentHashMap.newKeySet();

    // A stock change only marks the product dirty, and only after its transaction commits.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onStockChanged(StockChanged event) {
        dirty.add(event.getProductId());
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    void markAllDirtyOnStartup() {
        dirty.addAll(List.of("P100", "P200", "P300"));
    }

    // Called by the outbox job once Tiangge has every decision. Always sends the current real stock.
    void flushStock() {
        if (dirty.isEmpty()) {
            return;
        }
        List<String> batch = new ArrayList<>(dirty);
        dirty.removeAll(batch);
        try {
            List<TiangeJson.StockUpdate> updates = new ArrayList<>();
            for (String productId : batch) {
                updates.add(new TiangeJson.StockUpdate(productId, inventoryService.getItem(productId).getStock()));
            }
            client.publishStock(updates);
        } catch (Exception e) {
            dirty.addAll(batch);
            System.out.println("Stock sync failed, will retry: " + e.getMessage());
        }
    }
}