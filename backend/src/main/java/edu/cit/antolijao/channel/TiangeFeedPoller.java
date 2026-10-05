package edu.cit.antolijao.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
class TiangeFeedPoller {

    @Autowired
    private TiangeClient client;

    @Autowired
    private ChannelCursorRepository cursorRepository;

    @Autowired
    private TiangeOrderRepository tiangeOrderRepository;

    @Autowired
    private TiangeOrderDecisionHandler decisionHandler;

    @Scheduled(fixedDelay = 5000)
    void pollFeed() {
        try {
            long cursor = getCursor();
            TiangeJson.FeedResponse response = client.getFeed(cursor, 20);
            System.out.println("Polled feed at cursor=" + cursor + ", nextCursor=" + response.nextCursor + ", got " +
            (response.events == null ? 0 : response.events.size()) + " event(s)");

            if (response.events == null || response.events.isEmpty()) {
                return;
            }

            for (TiangeJson.FeedEvent event : response.events) {
                processEvent(event);
            }

            saveCursor(response.nextCursor);

        } catch (Exception e) {
            System.out.println("Feed poll failed: " + e.getMessage());
        }
    }

    private void processEvent(TiangeJson.FeedEvent event) {
        if ("ORDER_PLACED".equals(event.type)) {
            handleOrderPlaced(event);
        } else if ("ORDER_CANCELLED".equals(event.type)) {
            decisionHandler.handleCancellation(event);
        }
    }

    @Transactional
    void handleOrderPlaced(TiangeJson.FeedEvent event) {
        // Dedup by Tiangge's orderId (stable across redelivery), not eventId.
        Optional<TiangeOrder> existing = tiangeOrderRepository.findById(event.orderId);
        if (existing.isPresent()) {
            return; // already processed this order, ignore redelivery
        }
        decisionHandler.handleNewOrder(event);
    }

    private long getCursor() {
        return cursorRepository.findById(1)
                .map(ChannelCursor::getLastSeq)
                .orElse(0L);
    }

    private void saveCursor(long newSeq) {
        ChannelCursor cursor = cursorRepository.findById(1).orElseGet(ChannelCursor::new);
        cursor.setLastSeq(newSeq);
        cursorRepository.save(cursor);
    }
}