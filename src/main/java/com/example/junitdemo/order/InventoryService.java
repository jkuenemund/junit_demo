package com.example.junitdemo.order;

public interface InventoryService {

    boolean isAvailable(String productId, int quantity);

    void reserve(String productId, int quantity);
}
