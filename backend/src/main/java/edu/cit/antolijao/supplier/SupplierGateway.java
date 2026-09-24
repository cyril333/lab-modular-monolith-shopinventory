package edu.cit.antolijao.supplier;

public interface SupplierGateway {

    /**
     * Places a reorder for the given product and quantity (in OUR units).
     * Internally converts to LegacySupply's units, handles sessions, retries, and idempotency.
     * Returns our own domain result — never anything from LegacySupply's XML shape.
     */
    SupplierOrderResult placeReorder(String productId, int unitsNeeded, String buyerRef);
}