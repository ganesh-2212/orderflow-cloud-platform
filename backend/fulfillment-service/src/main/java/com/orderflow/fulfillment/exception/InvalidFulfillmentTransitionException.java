package com.orderflow.fulfillment.exception;

public class InvalidFulfillmentTransitionException extends RuntimeException {
    public InvalidFulfillmentTransitionException(String message) {
        super(message);
    }
}
