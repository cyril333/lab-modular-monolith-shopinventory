package edu.cit.antolijao.supplier;

public enum SupplierOrderStatus {
    PENDING,     // not yet sent to LegacySupply (queued for retry)
    ACCEPTED,    // LegacySupply returned 201 (their StatusCode 10)
    PICKING,     // their StatusCode 20
    SHIPPED,     // their StatusCode 30
    DELIVERED,   // their StatusCode 40
    FAILED       // exhausted retries / unrecoverable error
}