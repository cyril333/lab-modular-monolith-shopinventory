package edu.cit.antolijao.supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LegacySupplyConfig {

    @Value("${legacysupply.base-url}")
    String baseUrl;

    @Value("${legacysupply.client-id}")
    String clientId;

    @Value("${legacysupply.api-key}")
    String apiKey;
}