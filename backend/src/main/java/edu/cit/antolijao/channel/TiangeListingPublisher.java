package edu.cit.antolijao.channel;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class TiangeListingPublisher {

    @Autowired
    private TiangeClient client;

    @PostConstruct
    void publishOnStartup() {
        try {
            List<TiangeJson.ListingRequest> listings = List.of(
                    new TiangeJson.ListingRequest("P100", "Wireless Mouse", "NBR-3374"),
                    new TiangeJson.ListingRequest("P200", "Mechanical Keyboard", "NBR-8036"),
                    new TiangeJson.ListingRequest("P300", "USB-C Hub", "NBR-1384")
            );
            client.publishListings(listings);
            System.out.println("Published " + listings.size() + " listings to Tiangge");
        } catch (Exception e) {
            System.out.println("Failed to publish listings: " + e.getMessage());
        }
    }
}