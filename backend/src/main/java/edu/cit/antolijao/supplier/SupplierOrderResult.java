package edu.cit.antolijao.supplier;

public class SupplierOrderResult {
    private final boolean success;
    private final String poNumber;
    private final String message;

    private SupplierOrderResult(boolean success, String poNumber, String message) {
        this.success = success;
        this.poNumber = poNumber;
        this.message = message;
    }

    public static SupplierOrderResult accepted(String poNumber) {
        return new SupplierOrderResult(true, poNumber, "Accepted");
    }

    public static SupplierOrderResult pending(String message) {
        return new SupplierOrderResult(false, null, message);
    }

    public boolean isSuccess() { return success; }
    public String getPoNumber() { return poNumber; }
    public String getMessage() { return message; }
}