package edu.cit.antolijao.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class TiangeConfig {

    @Value("${tiangge.base-url}")
    String baseUrl;

    @Value("${tiangge.client-id}")
    String clientId;

    @Value("${tiangge.api-key}")
    String apiKey;
}