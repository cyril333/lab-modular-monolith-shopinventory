package edu.cit.antolijao.shop;

import java.util.List;

public class OrderResponse {
    private String status;
    private String reason;
    private List<ItemOutcome> items;
    private Integer inventory;

    public OrderResponse(String status, String reason, List<ItemOutcome> items) {
        this.status = status;
        this.reason = reason;
        this.items = items;
    }

    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public List<ItemOutcome> getItems() { return items; }
    public Integer getInventory() { return inventory; }
    public void setInventory(Integer inventory) { this.inventory = inventory; }

    public static class ItemOutcome {
    private String productId;
    private String productName;
    private String outcome;

    public ItemOutcome(String productId, String productName, String outcome) {
        this.productId = productId;
        this.productName = productName;
        this.outcome = outcome;
    }

    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getOutcome() { return outcome; }
    }
}