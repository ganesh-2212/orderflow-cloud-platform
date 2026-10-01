package com.orderflow.fulfillment.exception;

public class FulfillmentAlreadyExistsException extends RuntimeException {
    public FulfillmentAlreadyExistsException(String message) {
        super(message);
    }
}
