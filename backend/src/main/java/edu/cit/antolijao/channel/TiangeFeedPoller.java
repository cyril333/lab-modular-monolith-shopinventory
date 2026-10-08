package edu.cit.antolijao.channel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
class TiangeFeedPoller {

    @Autowired private TiangeClient client;
    @Autowired private ChannelCursorRepository cursorRepository;
    @Autowired private TiangeOrderDecisionHandler decisionHandler;

    private final Map<String, Integer> failures = new HashMap<>();

    @Scheduled(fixedDelay = 3000)
    void pollFeed() {
        try {
            long cursor = getCursor();
            TiangeJson.FeedResponse response = client.getFeed(cursor, 20);
            if (response == null || response.events == null || response.events.isEmpty()) {
                return;
            }
            System.out.println("Polled feed at cursor=" + cursor + ", got " + response.events.size() + " event(s)");

            for (TiangeJson.FeedEvent event : response.events) {
                try {
                    processEvent(event);
                } catch (Exception e) {
                    int n = failures.merge(String.valueOf(event.eventId), 1, Integer::sum);
                    System.out.println("Event " + event.eventId + " (" + event.orderId + ") failed locally (" + n + "): " + e.getMessage());
                    if (n < 5) {
                        return; // retried on the next poll; cursor stays before this event
                    }
                    System.out.println("Skipping poisoned event " + event.eventId);
                }
                saveCursor(event.seq); // local work is committed, so the cursor can move
            }
            saveCursor(response.nextCursor);
        } catch (Exception e) {
            System.out.println("Feed poll failed: " + e.getMessage());
        }
    }

    private void processEvent(TiangeJson.FeedEvent event) {
        synchronized (TiangeOrderDecisionHandler.LOCK) {
            if ("ORDER_PLACED".equals(event.type)) {
                decisionHandler.handleNewOrder(event);
            } else if ("ORDER_CANCELLED".equals(event.type)) {
                decisionHandler.handleCancellation(event);
            }
        }
    }

    private long getCursor() {
        return cursorRepository.findById(1).map(ChannelCursor::getLastSeq).orElse(0L);
    }

    private void saveCursor(long newSeq) {
        ChannelCursor cursor = cursorRepository.findById(1).orElseGet(ChannelCursor::new);
        cursor.setLastSeq(newSeq);
        cursorRepository.save(cursor);
    }
}