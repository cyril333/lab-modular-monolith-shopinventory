package edu.cit.antolijao.channel;

import edu.cit.antolijao.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class TiangeListingPublisher {

    @Autowired
    private TiangeClient client;

    @Autowired
    private InventoryService inventoryService;

    @EventListener(ApplicationReadyEvent.class)
    void publishOnStartup() {
        try {
            List<TiangeJson.ListingRequest> listings = List.of(
                    new TiangeJson.ListingRequest("P100", "Wireless Mouse", "NBR-3374"),
                    new TiangeJson.ListingRequest("P200", "Mechanical Keyboard", "NBR-8036"),
                    new TiangeJson.ListingRequest("P300", "USB-C Hub", "NBR-1384")
            );
            client.publishListings(listings);

            List<TiangeJson.StockUpdate> stock = new ArrayList<>();
            for (TiangeJson.ListingRequest l : listings) {
                stock.add(new TiangeJson.StockUpdate(l.sellerSku, inventoryService.getItem(l.sellerSku).getStock()));
            }
            client.publishStock(stock);

            System.out.println("Published " + listings.size() + " listings and initial stock to Tiangge");
        } catch (Exception e) {
            System.out.println("Failed to publish listings/stock: " + e.getMessage());
        }
    }
}