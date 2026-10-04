package edu.cit.antolijao.channel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
class TiangeJson {

    static class HeartbeatRequest {
        public String appName;
        public String startedAt;
        public long uptimeSeconds;

        HeartbeatRequest(String appName, String startedAt, long uptimeSeconds) {
            this.appName = appName;
            this.startedAt = startedAt;
            this.uptimeSeconds = uptimeSeconds;
        }
    }

    static class HeartbeatResponse {
        public String serverTime;
        public int nextHeartbeatSeconds;
    }

    static class ListingRequest {
        public String sellerSku;
        public String title;
        public String supplierSku;

        ListingRequest(String sellerSku, String title, String supplierSku) {
            this.sellerSku = sellerSku;
            this.title = title;
            this.supplierSku = supplierSku;
        }
    }

    static class StockUpdate {
        public String sellerSku;
        public int available;

        StockUpdate(String sellerSku, int available) {
            this.sellerSku = sellerSku;
            this.available = available;
        }
    }

    static class FeedResponse {
        public List<FeedEvent> events;
        public long nextCursor;
    }

    static class FeedEvent {
        public long seq;
        public String eventId;
        public String type; // ORDER_PLACED | ORDER_CANCELLED
        public String orderId;
        public String placedAt;
        public String decisionDeadline;
        public List<FeedLine> lines;
        public String cancelledAt;
        public String confirmDeadline;
    }

    static class FeedLine {
        public String sellerSku;
        public int qty;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class DecisionRequest {
        public String decision; // ACCEPTED | REJECTED | BACKORDERED
        public String shopOrderId;
        public String reason;

        DecisionRequest(String decision, String shopOrderId, String reason) {
            this.decision = decision;
            this.shopOrderId = shopOrderId;
            this.reason = reason;
        }
    }

    static class ResolutionRequest {
        public String status; // ACCEPTED | CANCELLED

        ResolutionRequest(String status) {
            this.status = status;
        }
    }

    static class CancellationConfirmRequest {
        public boolean restocked;

        CancellationConfirmRequest(boolean restocked) {
            this.restocked = restocked;
        }
    }

    static class ErrorResponse {
        public String error;
        public String message;
    }
}