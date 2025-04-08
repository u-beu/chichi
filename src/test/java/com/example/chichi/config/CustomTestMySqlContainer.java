package com.example.chichi.config;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;

public class CustomTestMySqlContainer {
    public static final MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("chichi")
            .withUsername("mock-root")
            .withPassword("1234");

    public static void setup(DynamicPropertyRegistry registry) {
        mysql.start();
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
