package com.example.junitdemo.order;

import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(String id);
}
