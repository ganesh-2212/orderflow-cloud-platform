package com.orderflow.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Component
public class FulfillmentClient {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentClient.class);
    
    private final WebClient webClient;
    private final long timeoutMs;

    public FulfillmentClient(
            WebClient.Builder webClientBuilder,
            @Value("${fulfillment.service.url}") String fulfillmentServiceUrl,
            @Value("${fulfillment.service.timeout-ms:5000}") long timeoutMs) {
        this.webClient = webClientBuilder.baseUrl(fulfillmentServiceUrl).build();
        this.timeoutMs = timeoutMs;
    }

    public void createFulfillment(Long orderId, String warehouseId) {
        log.info("Sending create fulfillment request for orderId {} to warehouse {}", orderId, warehouseId);
        try {
            webClient.post()
                    .uri("/api/v1/fulfillments")
                    .bodyValue(Map.of("orderId", orderId, "warehouseId", warehouseId))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
            log.info("Successfully created fulfillment for orderId {}", orderId);
        } catch (WebClientResponseException e) {
            log.error("Fulfillment service rejected creation for orderId {}: {} - {}", 
                    orderId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new FulfillmentClientException("Fulfillment service rejected creation: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to connect or timeout to fulfillment service for orderId {}: {}", orderId, e.getMessage());
            throw new FulfillmentClientException("Failed to reach fulfillment service: " + e.getMessage());
        }
    }
}
