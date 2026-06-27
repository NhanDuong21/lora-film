package com.project.promotionservice.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

@SpringBootTest
@SuppressWarnings("resource")
public abstract class BaseIntegrationTest {

    protected static final MySQLContainer<?> mysql;
    protected static final boolean USE_LOCAL_MYSQL;

    static {
        boolean useLocal = false;
        MySQLContainer<?> container = null;
        try {
            container = new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("promotion_db")
                    .withUsername("root")
                    .withPassword("THGaming809");
            container.start();
            // Register JVM shutdown hook to close the container when tests finish
            final MySQLContainer<?> finalContainer = container;
            Runtime.getRuntime().addShutdownHook(new Thread(finalContainer::close));
        } catch (Exception e) {
            System.err.println("Testcontainers failed to initialize: " + e.getMessage());
            System.err.println("Falling back to local MySQL container on port 3307...");
            useLocal = true;
            if (container != null) {
                try {
                    container.close();
                } catch (Exception ignored) {}
                container = null;
            }
        }
        mysql = container;
        USE_LOCAL_MYSQL = useLocal;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (USE_LOCAL_MYSQL) {
            String port = System.getenv("DB_PORT") != null ? System.getenv("DB_PORT") : (System.getenv("MYSQL_PORT") != null ? System.getenv("MYSQL_PORT") : "3306");
            registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:" + port + "/promotion_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true");
            registry.add("spring.datasource.username", () -> "root");
            registry.add("spring.datasource.password", () -> "THGaming809");
        } else {
            registry.add("spring.datasource.url", mysql::getJdbcUrl);
            registry.add("spring.datasource.username", mysql::getUsername);
            registry.add("spring.datasource.password", mysql::getPassword);
        }
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.jpa.show-sql", () -> "true");
    }
}
