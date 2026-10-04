package edu.cit.antolijao.inventory;

public class StockChanged {
    private final String productId;
    private final int newStock;

    public StockChanged(String productId, int newStock) {
        this.productId = productId;
        this.newStock = newStock;
    }

    public String getProductId() { return productId; }
    public int getNewStock() { return newStock; }
}