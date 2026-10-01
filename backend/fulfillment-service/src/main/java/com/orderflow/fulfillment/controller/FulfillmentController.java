package com.orderflow.fulfillment.controller;

import com.orderflow.fulfillment.dto.request.CreateFulfillmentRequest;
import com.orderflow.fulfillment.dto.request.UpdateFulfillmentStatusRequest;
import com.orderflow.fulfillment.dto.response.FulfillmentResponse;
import com.orderflow.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fulfillments")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping
    public ResponseEntity<FulfillmentResponse> createFulfillment(@Valid @RequestBody CreateFulfillmentRequest request) {
        FulfillmentResponse response = fulfillmentService.createFulfillment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FulfillmentResponse> getFulfillmentById(@PathVariable Long id) {
        FulfillmentResponse response = fulfillmentService.getFulfillmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<FulfillmentResponse> getFulfillmentByOrderId(@PathVariable Long orderId) {
        FulfillmentResponse response = fulfillmentService.getFulfillmentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<FulfillmentResponse>> getAllFulfillments() {
        List<FulfillmentResponse> response = fulfillmentService.getAllFulfillments();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FulfillmentResponse> updateFulfillmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFulfillmentStatusRequest request) {
        FulfillmentResponse response = fulfillmentService.updateFulfillmentStatus(id, request.getStatus());
        return ResponseEntity.ok(response);
    }
}
