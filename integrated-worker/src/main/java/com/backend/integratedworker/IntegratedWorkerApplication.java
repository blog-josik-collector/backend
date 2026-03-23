package com.backend.integratedworker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.backend")
public class IntegratedWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegratedWorkerApplication.class, args);
    }
}
