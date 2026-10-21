package com.jbetting.app;

import org.springframework.boot.SpringApplication;

// Runs the application locally against a throwaway PostgreSQL container.
public class TestJbettingApplication {

    public static void main(String[] args) {
        SpringApplication.from(JbettingApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
