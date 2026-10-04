package edu.cit.antolijao.channel;

import edu.cit.antolijao.inventory.StockChanged;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class TiangeStockSyncListener {

    @Autowired
    private TiangeClient client;

    @EventListener
    void onStockChanged(StockChanged event) {
        try {
            client.publishStock(List.of(
                    new TiangeJson.StockUpdate(event.getProductId(), event.getNewStock())
            ));
        } catch (Exception e) {
            System.out.println("Failed to sync stock to Tiangge for " + event.getProductId() + ": " + e.getMessage());
        }
    }
}