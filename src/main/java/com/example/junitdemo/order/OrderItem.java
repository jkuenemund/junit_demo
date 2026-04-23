package com.example.junitdemo.order;

import lombok.Getter;

@Getter
public class OrderItem {

    private final Product product;
    private final int quantity;

    public OrderItem(Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, was: " + quantity);
        }
        this.product = product;
        this.quantity = quantity;
    }

    public Money totalPrice() {
        return product.getPrice().multiply(quantity);
    }
}
