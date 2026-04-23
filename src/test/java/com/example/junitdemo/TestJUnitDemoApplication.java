package com.example.junitdemo;

import org.springframework.boot.SpringApplication;

public class TestJUnitDemoApplication {

    public static void main(String[] args) {
        SpringApplication.from(JUnitDemoApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
