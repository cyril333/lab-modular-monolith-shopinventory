package edu.cit.antolijao.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

@Component
class TiangeClient {

    @Autowired
    private TiangeConfig config;

    @Autowired
    private ApplicationInstance instance;

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    TiangeClient() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    private RestClient.RequestBodySpec authedPost(String path) {
        return (RestClient.RequestBodySpec) restClient.post()
                .uri(config.baseUrl + path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-Id", config.clientId)
                .header("Authorization", "Bearer " + config.apiKey)
                .header("X-Client-Instance", instance.getInstanceId());
    }

    private RestClient.RequestHeadersSpec<?> authedGet(String path) {
        return restClient.get()
                .uri(config.baseUrl + path)
                .header("X-Client-Id", config.clientId)
                .header("Authorization", "Bearer " + config.apiKey)
                .header("X-Client-Instance", instance.getInstanceId());
    }

    private RestClient.RequestBodySpec authedPut(String path) {
        return (RestClient.RequestBodySpec) restClient.put()
                .uri(config.baseUrl + path)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Client-Id", config.clientId)
                .header("Authorization", "Bearer " + config.apiKey)
                .header("X-Client-Instance", instance.getInstanceId());
    }

    TiangeJson.HeartbeatResponse sendHeartbeat(TiangeJson.HeartbeatRequest req) {
        return call(() -> authedPost("/instances/heartbeat")
                .body(toJson(req))
                .retrieve()
                .body(TiangeJson.HeartbeatResponse.class));
    }

    void publishListings(List<TiangeJson.ListingRequest> listings) {
        call(() -> {
            authedPut("/listings").body(toJson(listings)).retrieve().toBodilessEntity();
            return null;
        });
    }

    void publishStock(List<TiangeJson.StockUpdate> updates) {
        call(() -> {
            authedPut("/stock").body(toJson(updates)).retrieve().toBodilessEntity();
            return null;
        });
    }

    TiangeJson.FeedResponse getFeed(long after, int limit) {
        return call(() -> authedGet("/feed?after=" + after + "&limit=" + limit)
                .retrieve()
                .body(TiangeJson.FeedResponse.class));
    }

    void decide(String orderId, TiangeJson.DecisionRequest req) {
        call(() -> {
            authedPost("/orders/" + orderId + "/decision").body(toJson(req)).retrieve().toBodilessEntity();
            return null;
        });
    }

    void resolve(String orderId, TiangeJson.ResolutionRequest req) {
        call(() -> {
            authedPost("/orders/" + orderId + "/resolution").body(toJson(req)).retrieve().toBodilessEntity();
            return null;
        });
    }

    void confirmCancellation(String orderId, TiangeJson.CancellationConfirmRequest req) {
        call(() -> {
            authedPost("/orders/" + orderId + "/cancellation").body(toJson(req)).retrieve().toBodilessEntity();
            return null;
        });
    }

    private <T> T call(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException e) {
            String code = extractErrorCode(e);
            throw new TiangeException(code, e.getStatusCode().value(), e);
        }
    }

    private String extractErrorCode(RestClientResponseException e) {
        try {
            TiangeJson.ErrorResponse err = mapper.readValue(e.getResponseBodyAsString(), TiangeJson.ErrorResponse.class);
            return err.error;
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize request", e);
        }
    }
}