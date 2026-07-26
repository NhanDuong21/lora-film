package com.project.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AuthServiceApplication {

	/**
	 * Starts the auth-service Spring Boot application.
	 *
	 * @param args application arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}
}
