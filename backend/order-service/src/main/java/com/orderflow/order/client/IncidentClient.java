package com.orderflow.order.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class IncidentClient {

    private static final Logger log = LoggerFactory.getLogger(IncidentClient.class);

    private final WebClient webClient;
    private final long timeoutMs;

    public IncidentClient(
            WebClient.Builder webClientBuilder,
            @Value("${incident.service.url}") String incidentServiceUrl,
            @Value("${incident.service.timeout-ms:5000}") long timeoutMs) {
        this.webClient = webClientBuilder.baseUrl(incidentServiceUrl).build();
        this.timeoutMs = timeoutMs;
    }

    public void reportIncident(IncidentRequest request) {
        log.info("Reporting incident: resourceId={}, category={}", request.getResourceId(), request.getCategory());
        try {
            webClient.post()
                    .uri("/api/v1/incidents")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
            log.info("Incident reported: resourceId={}", request.getResourceId());
        } catch (Exception e) {
            log.error("Incident reporting failed: resourceId={}, error={}", request.getResourceId(), e.getMessage());
            // We DO NOT throw an exception here because incident reporting is best-effort.
            // Throwing would overwrite the original business failure.
        }
    }
}
