package com.jbetting.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// The runnable solution: it starts every bounded context, each one bringing its own configuration.
@SpringBootApplication(scanBasePackages = "com.jbetting")
public class JbettingApplication {

    public static void main(String[] args) {
        SpringApplication.run(JbettingApplication.class, args);
    }
}
