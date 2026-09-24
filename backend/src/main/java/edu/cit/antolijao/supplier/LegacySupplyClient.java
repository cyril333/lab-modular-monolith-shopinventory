package edu.cit.antolijao.supplier;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.StringReader;
import java.io.StringWriter;
import java.time.Duration;

@Component
class LegacySupplyClient {

    @Autowired
    private LegacySupplyConfig config;

    @Autowired
    private LegacySupplySession session;

    private final RestClient restClient;

    LegacySupplyClient() {
        this.restClient = RestClient.builder()
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    private org.springframework.http.client.ClientHttpRequestFactory clientHttpRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
        return factory;
    }

    void authenticate() {
        LegacySupplyXml.AuthRequest req = new LegacySupplyXml.AuthRequest(config.clientId, config.apiKey);
        String body = marshal(req);

        String responseBody = restClient.post()
                .uri(config.baseUrl + "/auth/token")
                .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                .body(body)
                .retrieve()
                .body(String.class);

        LegacySupplyXml.AuthResponse response = unmarshal(responseBody, LegacySupplyXml.AuthResponse.class);
        session.set(response.sessionToken);
    }

    LegacySupplyXml.PurchaseOrderAck placeOrder(String supplierSku, int qty, String buyerRef, String requestId) {
        if (!session.hasToken()) {
            authenticate();
        }
        return doPlaceOrder(supplierSku, qty, buyerRef, requestId, true);
    }

    private LegacySupplyXml.PurchaseOrderAck doPlaceOrder(
            String supplierSku, int qty, String buyerRef, String requestId, boolean allowReauth
    ) {
        LegacySupplyXml.PurchaseOrderRequest req = new LegacySupplyXml.PurchaseOrderRequest(supplierSku, qty, buyerRef);
        String body = marshal(req);

        try {
            String responseBody = restClient.post()
                    .uri(config.baseUrl + "/purchase-orders")
                    .contentType(org.springframework.http.MediaType.APPLICATION_XML)
                    .header("X-LS-Session", session.getToken())
                    .header("X-Request-Id", requestId)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return unmarshal(responseBody, LegacySupplyXml.PurchaseOrderAck.class);

        } catch (RestClientResponseException e) {
            if (isAuthError(e) && allowReauth) {
                session.clear();
                authenticate();
                return doPlaceOrder(supplierSku, qty, buyerRef, requestId, false);
            }
            throw new LegacySupplyException(extractErrorCode(e), e.getStatusCode().value(), e);
        }
    }

    LegacySupplyXml.PurchaseOrderStatusXml getOrderStatus(String poNumber) {
        if (!session.hasToken()) {
            authenticate();
        }
        try {
            String responseBody = restClient.get()
                    .uri(config.baseUrl + "/purchase-orders/" + poNumber)
                    .header("X-LS-Session", session.getToken())
                    .retrieve()
                    .body(String.class);
            return unmarshal(responseBody, LegacySupplyXml.PurchaseOrderStatusXml.class);
        } catch (RestClientResponseException e) {
            if (isAuthError(e)) {
                session.clear();
                authenticate();
                String responseBody = restClient.get()
                        .uri(config.baseUrl + "/purchase-orders/" + poNumber)
                        .header("X-LS-Session", session.getToken())
                        .retrieve()
                        .body(String.class);
                return unmarshal(responseBody, LegacySupplyXml.PurchaseOrderStatusXml.class);
            }
            throw new LegacySupplyException(extractErrorCode(e), e.getStatusCode().value(), e);
        }
    }

    private boolean isAuthError(RestClientResponseException e) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        return status == HttpStatus.UNAUTHORIZED;
    }

    private String extractErrorCode(RestClientResponseException e) {
        try {
            LegacySupplyXml.LSError error = unmarshal(e.getResponseBodyAsString(), LegacySupplyXml.LSError.class);
            return error.code;
        } catch (Exception ex) {
            return "UNKNOWN";
        }
    }

    private String marshal(Object obj) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(obj.getClass());
            Marshaller m = ctx.createMarshaller();
            StringWriter writer = new StringWriter();
            m.marshal(obj, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to build XML request", e);
        }
    }

    private <T> T unmarshal(String xml, Class<T> type) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(type);
            Unmarshaller u = ctx.createUnmarshaller();
            return type.cast(u.unmarshal(new StringReader(xml)));
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to parse XML response", e);
        }
    }
}