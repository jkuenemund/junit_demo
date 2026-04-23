package com.example.junitdemo.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Product {

    private final String id;
    private final String name;
    private final Money price;
}
