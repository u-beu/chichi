package com.example.chichi.config;

import org.testcontainers.containers.MySQLContainer;

public class CustomTestMySqlContainer {
    public static final MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("chichi")
            .withUsername("mock-root")
            .withPassword("1234");

    public static void setup() {
        mysql.start();
    }
}
