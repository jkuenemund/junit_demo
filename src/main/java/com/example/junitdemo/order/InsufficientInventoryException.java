package com.example.junitdemo.order;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(String productId, int requested) {
        super("Insufficient inventory for product '" + productId + "': requested " + requested);
    }
}
