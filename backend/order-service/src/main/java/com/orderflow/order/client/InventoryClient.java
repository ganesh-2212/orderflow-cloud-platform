package com.orderflow.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
    
    private final WebClient webClient;
    private final long timeoutMs;

    public InventoryClient(
            WebClient.Builder webClientBuilder,
            @Value("${inventory.service.url}") String inventoryServiceUrl,
            @Value("${inventory.service.timeout-ms:5000}") long timeoutMs) {
        this.webClient = webClientBuilder.baseUrl(inventoryServiceUrl).build();
        this.timeoutMs = timeoutMs;
    }

    public void reserveStock(String productId, Integer quantity) {
        log.info("Sending reserve stock request for product {} quantity {}", productId, quantity);
        try {
            webClient.post()
                    .uri("/api/v1/inventory/reserve")
                    .bodyValue(Map.of("productId", productId, "quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
            log.info("Successfully reserved stock for product {}", productId);
        } catch (WebClientResponseException e) {
            log.error("Inventory service rejected reservation for product {}: {} - {}", 
                    productId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new InventoryClientException("Inventory service rejected reservation: " + e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to connect or timeout to inventory service for product {}: {}", productId, e.getMessage());
            throw new InventoryClientException("Failed to reach inventory service: " + e.getMessage());
        }
    }

    public void releaseStock(String productId, Integer quantity) {
        log.info("Sending release stock request for product {} quantity {}", productId, quantity);
        try {
            webClient.post()
                    .uri("/api/v1/inventory/release")
                    .bodyValue(Map.of("productId", productId, "quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
            log.info("Successfully released stock for product {}", productId);
        } catch (Exception e) {
            log.error("Failed to release stock for product {}. Manual intervention might be needed: {}", 
                    productId, e.getMessage());
            // We usually don't rethrow on release since it's a compensation action, 
            // but we log heavily.
        }
    }
}
