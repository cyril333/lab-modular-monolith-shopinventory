package edu.cit.antolijao.shop;

public class OrderResponse {
    private String status;
    private String reason;
    private Integer inventory;

    public OrderResponse(String status, String reason, Integer inventory) {
        this.status = status;
        this.reason = reason;
        this.inventory = inventory;
    }

    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public Integer getInventory() { return inventory; }
}