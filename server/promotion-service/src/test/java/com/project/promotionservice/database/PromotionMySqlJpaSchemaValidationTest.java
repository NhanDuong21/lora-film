package com.project.promotionservice.database;

import jakarta.persistence.EntityManagerFactory;
import com.project.promotionservice.automation.entity.PromotionAudienceMember;
import com.project.promotionservice.automation.entity.PromotionPlaybook;
import com.project.promotionservice.automation.enums.PlaybookStatus;
import com.project.promotionservice.automation.repository.PromotionAudienceMemberRepository;
import com.project.promotionservice.automation.repository.PromotionPlaybookRepository;
import com.project.promotionservice.automation.service.PromotionAutomationBudgetService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@ContextConfiguration(initializers =
        PromotionMySqlJpaSchemaValidationTest.SchemaInitializer.class)
class PromotionMySqlJpaSchemaValidationTest {

    @Container
    private static final MySQLContainer<?> MYSQL =
            new MySQLContainer<>("mysql:8.4")
                    .withDatabaseName("promotion_db")
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
    @Autowired private PromotionPlaybookRepository playbookRepository;
    @Autowired private PromotionAudienceMemberRepository memberRepository;
    @Autowired private PromotionAutomationBudgetService budgetService;

    @Test
    void canonicalSqlSchemaMatchesEveryJpaEntity() throws Exception {
        assertThat(entityManagerFactory).isNotNull();
        try (Connection connection = dataSource.getConnection();
             ResultSet result = connection.createStatement().executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables "
                             + "WHERE table_schema = DATABASE() "
                             + "AND table_name = 'flyway_schema_history'")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isZero();
        }
    }

    @Test
    void concurrentWorkersCannotOverspendTheSameMonthlyPlaybookBudget() throws Exception {
        PromotionPlaybook playbook = new PromotionPlaybook();
        playbook.setCode("CONCURRENCY_BUDGET_TEST");
        playbook.setName("Concurrency budget test");
        playbook.setTriggerType("TEST");
        playbook.setConfigJson("{}");
        playbook.setScopeJson("{}");
        playbook.setBudgetLimit(new BigDecimal("5000000.00"));
        playbook.setQuotaLimit(2);
        playbook.setStatus(PlaybookStatus.ACTIVE);
        playbook.setCreatedBy("TEST");
        playbook.setUpdatedBy("TEST");
        playbook = playbookRepository.saveAndFlush(playbook);

        PromotionAudienceMember first = member("run-budget-test", "customer-a", "issue-a");
        PromotionAudienceMember second = member("run-budget-test", "customer-b", "issue-b");
        memberRepository.saveAllAndFlush(List.of(first, second));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            String playbookId = playbook.getPublicId();
            Future<PromotionAutomationBudgetService.ReservationResult> one = executor.submit(
                    () -> reserveTogether(ready, start, first.getPublicId(), playbookId));
            Future<PromotionAutomationBudgetService.ReservationResult> two = executor.submit(
                    () -> reserveTogether(ready, start, second.getPublicId(), playbookId));
            ready.await();
            start.countDown();
            List<PromotionAutomationBudgetService.ReservationResult> results =
                    List.of(one.get(), two.get());
            assertThat(results).containsExactlyInAnyOrder(
                    PromotionAutomationBudgetService.ReservationResult.RESERVED,
                    PromotionAutomationBudgetService.ReservationResult.BUDGET_EXHAUSTED);
        }

        PromotionPlaybook reloaded = playbookRepository
                .findByPublicIdAndDeletedAtIsNull(playbook.getPublicId()).orElseThrow();
        assertThat(reloaded.getBudgetCommitted())
                .isEqualByComparingTo("4000000.00");
        assertThat(reloaded.getQuotaCommitted()).isEqualTo(1);
    }

    private PromotionAutomationBudgetService.ReservationResult reserveTogether(
            CountDownLatch ready, CountDownLatch start,
            String memberId, String playbookId) throws Exception {
        ready.countDown();
        start.await();
        return budgetService.reserveForMember(
                memberId, playbookId, new BigDecimal("4000000.00"));
    }

    private PromotionAudienceMember member(
            String runId, String customerId, String issuanceKey) {
        PromotionAudienceMember member = new PromotionAudienceMember();
        member.setSnapshotPublicId("snapshot-budget-test");
        member.setRunPublicId(runId);
        member.setCustomerPublicId(customerId);
        member.setIssuanceKey(issuanceKey);
        member.setCreatedBy("TEST");
        member.setUpdatedBy("TEST");
        return member;
    }

    static class SchemaInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            Path schema = findCanonicalSchema();
            try (Connection connection = DriverManager.getConnection(
                    MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())) {
                String schemaSql = Files.readString(schema, StandardCharsets.UTF_8);
                String useStatement = "USE promotion_db;";
                int ddlStart = schemaSql.indexOf(useStatement);
                if (ddlStart < 0) {
                    throw new IllegalStateException("Canonical schema has no USE promotion_db statement");
                }
                String tableDdl = schemaSql.substring(ddlStart + useStatement.length());
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(
                        tableDdl.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception exception) {
                throw new IllegalStateException(
                        "Could not apply canonical Promotion schema: " + schema, exception);
            }
        }

        private static Path findCanonicalSchema() {
            Path workingDirectory = Path.of(System.getProperty("user.dir"))
                    .toAbsolutePath()
                    .normalize();
            Path fromRepositoryRoot = workingDirectory.resolve(
                    "docs/database/mysql/promotion-service-schema.sql");
            if (Files.isRegularFile(fromRepositoryRoot)) return fromRepositoryRoot;

            Path fromServiceDirectory = workingDirectory.resolve(
                    "../../docs/database/mysql/promotion-service-schema.sql").normalize();
            if (Files.isRegularFile(fromServiceDirectory)) return fromServiceDirectory;

            throw new IllegalStateException(
                    "Cannot locate docs/database/mysql/promotion-service-schema.sql from "
                            + workingDirectory);
        }
    }
}
