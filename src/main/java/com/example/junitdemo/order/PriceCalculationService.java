package com.example.junitdemo.order;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceCalculationService {

    static final int BULK_DISCOUNT_THRESHOLD = 5;
    static final double BULK_DISCOUNT_PERCENT = 10.0;

    public Money calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(OrderItem::totalPrice)
            .reduce(Money.of("0.00", "EUR"), Money::add);
    }

    public Money calculateTotalWithDiscount(List<OrderItem> items, double discountPercent) {
        return calculateTotal(items).applyDiscount(discountPercent);
    }

    public double resolveDiscount(Order order) {
        int totalQuantity = order.getItems().stream()
            .mapToInt(OrderItem::getQuantity)
            .sum();
        return totalQuantity >= BULK_DISCOUNT_THRESHOLD ? BULK_DISCOUNT_PERCENT : 0.0;
    }
}
