package edu.cit.antolijao.channel;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class TiangeHeartbeatScheduler {

    @Autowired
    private TiangeClient client;

    @Autowired
    private ApplicationInstance instance;

    @PostConstruct
    void sendFirstHeartbeat() {
        sendHeartbeat();
    }

    @Scheduled(fixedDelay = 30000)
    void sendHeartbeat() {
        try {
            TiangeJson.HeartbeatRequest req = new TiangeJson.HeartbeatRequest(
                    "shopinventory",
                    instance.getStartedAt().toString(),
                    instance.uptimeSeconds()
            );
            client.sendHeartbeat(req);
        } catch (Exception e) {
            System.out.println("Heartbeat failed: " + e.getMessage());
        }
    }
}