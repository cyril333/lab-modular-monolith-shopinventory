package edu.cit.antolijao.channel;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ApplicationInstance {

    private final String instanceId = UUID.randomUUID().toString();
    private final OffsetDateTime startedAt = OffsetDateTime.now();

    public String getInstanceId() { return instanceId; }
    public OffsetDateTime getStartedAt() { return startedAt; }

    public long uptimeSeconds() {
        return java.time.Duration.between(startedAt, OffsetDateTime.now()).getSeconds();
    }
}