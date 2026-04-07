package com.example.chichi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ChichiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChichiApplication.class, args);
	}

}
