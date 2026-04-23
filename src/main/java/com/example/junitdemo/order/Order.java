package com.example.junitdemo.order;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class Order {

    private final String id;
    private final String customerId;
    private final List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;

    public Order(String customerId) {
        this.id = UUID.randomUUID().toString();
        this.customerId = customerId;
        this.status = OrderStatus.PENDING;
    }

    public void addItem(OrderItem item) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot modify a non-pending order, current status: " + status);
        }
        items.add(item);
    }

    public void confirm() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed, current status: " + status);
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot confirm an empty order");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw new IllegalStateException("Cannot cancel an already shipped order");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Money totalPrice() {
        return items.stream()
            .map(OrderItem::totalPrice)
            .reduce(Money.of("0.00", "EUR"), Money::add);
    }
}
