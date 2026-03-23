package com.backend.integratedapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.backend")
public class IntegratedApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegratedApiApplication.class, args);
    }
}
