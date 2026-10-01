package com.orderflow.fulfillment.service;

import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

@Component
public class TrackingNumberGenerator {
    
    private static int counter = 1;

    public synchronized String generate(Long fulfillmentId) {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tracking = String.format("TRK-%s-%05d", datePart, counter++);
        return tracking;
    }
}
