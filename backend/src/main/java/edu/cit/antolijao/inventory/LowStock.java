package edu.cit.antolijao.inventory;

public class LowStock {
    private final String productId;
    private final int remainingStock;

    public LowStock(String productId, int remainingStock) {
        this.productId = productId;
        this.remainingStock = remainingStock;
    }

    public String getProductId() { return productId; }
    public int getRemainingStock() { return remainingStock; }
}   