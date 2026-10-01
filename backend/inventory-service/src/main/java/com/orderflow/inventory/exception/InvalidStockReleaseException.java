package com.orderflow.inventory.exception;

public class InvalidStockReleaseException extends RuntimeException {
    public InvalidStockReleaseException(String message) {
        super(message);
    }
}
