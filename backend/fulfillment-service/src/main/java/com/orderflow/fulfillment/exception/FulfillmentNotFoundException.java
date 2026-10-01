package com.orderflow.fulfillment.exception;

public class FulfillmentNotFoundException extends RuntimeException {
    public FulfillmentNotFoundException(String message) {
        super(message);
    }
}
