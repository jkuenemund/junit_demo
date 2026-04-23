package com.example.junitdemo.order;

import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.inventoryService = inventoryService;
    }

    public Order createOrder(String customerId) {
        Order order = new Order(customerId);
        return orderRepository.save(order);
    }

    public Order addItemToOrder(String orderId, String productId, int quantity) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        if (!inventoryService.isAvailable(productId, quantity)) {
            throw new InsufficientInventoryException(productId, quantity);
        }

        order.addItem(new OrderItem(product, quantity));
        inventoryService.reserve(productId, quantity);
        return orderRepository.save(order);
    }

    public Order confirmOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.confirm();
        return orderRepository.save(order);
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        order.cancel();
        orderRepository.save(order);
    }
}
