package com.project.promotionservice.database;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PromotionMySqlJpaSchemaValidationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("promotion_service_jpa")
                    .withUsername("promotion")
                    .withPassword("promotion-test-password");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.database-platform",
                () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "1");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("eureka.client.register-with-eureka", () -> "false");
        registry.add("eureka.client.fetch-registry", () -> "false");
        registry.add("app.scheduling.enable", () -> "false");
        registry.add("promotion.integration.consumers-enabled", () -> "false");
        registry.add("promotion.reservation.distributed-lock-enabled", () -> "false");
        registry.add("jwt.secret",
                () -> "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        registry.add("app.internal-auth.booking-service-token",
                () -> "schema-booking-token");
        registry.add("app.internal-auth.payment-service-token",
                () -> "schema-payment-token");
        registry.add("app.internal-auth.operations-service-token",
                () -> "schema-operations-token");
    }

    @Autowired private EntityManagerFactory entityManagerFactory;
    @Autowired private DataSource dataSource;

    @Test
    void flywaySchemaMatchesEveryJpaEntity() throws Exception {
        assertThat(entityManagerFactory).isNotNull();
        try (Connection connection = dataSource.getConnection();
             ResultSet result = connection.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(9);
        }
    }
}
