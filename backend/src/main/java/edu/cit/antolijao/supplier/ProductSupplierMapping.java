package edu.cit.antolijao.supplier;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class ProductSupplierMapping {

    // TODO: replace with real SupplierSku/PackSize values once GET /catalog is confirmed (Part B)
    private static final Map<String, Mapping> MAPPINGS = Map.of(
        "P100", new Mapping("NBR-3374", 6),
        "P200", new Mapping("NBR-8036", 6),
        "P300", new Mapping("NBR-1384", 10)
    );

    Mapping forProduct(String productId) {
        Mapping mapping = MAPPINGS.get(productId);
        if (mapping == null) {
            throw new IllegalArgumentException("No supplier mapping for product: " + productId);
        }
        return mapping;
    }

    record Mapping(String supplierSku, int packSize) {}
}