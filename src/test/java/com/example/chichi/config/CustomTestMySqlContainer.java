package com.example.chichi.config;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class CustomTestMySqlContainer {

    public static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("chichi")
            .withUsername("mock-root")
            .withPassword("1234")
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));

    static {
        mySQLContainer.start();
    }
}
