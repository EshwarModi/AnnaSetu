package com.annasetu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Responsibility:
 * Main bootstrap entry point for the AnnaSetu Spring Boot application.
 *
 * Domain & Architecture Context:
 * - Bootstraps the embedded web server (Tomcat), Spring ApplicationContext, and Spring Data JPA repositories.
 * - Activates auto-configuration for Spring MVC, Spring Security 6, Hibernate Validator, and Thymeleaf.
 *
 * Interview Talking Points:
 * 1. {@code @SpringBootApplication} is a meta-annotation encapsulating {@code @Configuration},
 *    {@code @EnableAutoConfiguration}, and {@code @ComponentScan}.
 * 2. Packages under {@code com.annasetu} (config, controller, service, repository, entity, dto, exception, util)
 *    are automatically scanned and wired into the dependency injection container.
 */
@SpringBootApplication
public class AnnaSetuApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnnaSetuApplication.class, args);
    }
}
