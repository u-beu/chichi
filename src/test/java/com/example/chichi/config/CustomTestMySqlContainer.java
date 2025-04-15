package com.example.chichi.config;

import org.testcontainers.containers.MySQLContainer;

public class CustomTestMySqlContainer {

    public static final MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("chichi")
            .withUsername("mock-root")
            .withPassword("1234");

    static {
        mySQLContainer.start();
    }
}
