package edu.cit.antolijao.supplier;

public class SupplierOrderDelivered {

    private final String productId;
    private final int units;
    private final String poNumber;

    public SupplierOrderDelivered(String productId, int units, String poNumber) {
        this.productId = productId;
        this.units = units;
        this.poNumber = poNumber;
    }

    public String getProductId() {
        return productId;
    }

    public int getUnits() {
        return units;
    }

    public String getPoNumber() {
        return poNumber;
    }
}