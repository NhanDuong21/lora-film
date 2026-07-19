package com.lorafilm.movie.autoschedule.repository;

import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreview;
import com.lorafilm.movie.autoschedule.domain.enums.AutoScheduleStrategy;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewApplyMode;
import com.lorafilm.movie.autoschedule.domain.enums.SchedulePreviewStatus;
import com.lorafilm.movie.cinema.domain.entity.Cinema;
import com.lorafilm.movie.cinema.domain.enums.CinemaStatus;
import com.lorafilm.movie.cinema.repository.CinemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;
import com.lorafilm.movie.autoschedule.domain.entity.ShowtimeSchedulePreviewTestFactory;

@org.springframework.context.annotation.Import(com.lorafilm.movie.common.config.AuditConfig.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://127.0.0.1:3307/movie_db_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
    "spring.datasource.username=root",
    "spring.datasource.password=12345678",
    "spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect",
    "spring.jpa.hibernate.ddl-auto=update"
})
public class ShowtimeSchedulePreviewRepositoryIntegrationTest {

    @Autowired
    private ShowtimeSchedulePreviewRepository previewRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private Cinema cinema;

    @BeforeEach
    void setUp() {
        cinema = new Cinema();
        cinema.setName("Test Cinema " + UUID.randomUUID());
        cinema.setSlug("test-cinema-" + UUID.randomUUID());
        cinema.setTimezone("Asia/Ho_Chi_Minh");
        cinema.setStatus(CinemaStatus.ACTIVE);
        cinema.setAddress("123 Street");
        cinema.setCity("HCM");
        cinema.setDistrict("Q1");
        cinema.setPublicId(UUID.randomUUID().toString());
        cinema = cinemaRepository.saveAndFlush(cinema);
    }

    private ShowtimeSchedulePreview createValidPreview(String publicId, String genKey, String applyKey) {
        ShowtimeSchedulePreview preview = ShowtimeSchedulePreviewTestFactory.createPreview();
        preview.setPublicId(publicId);
        preview.setCinema(cinema);
        preview.setScheduleFrom(LocalDate.now());
        preview.setScheduleTo(LocalDate.now().plusDays(7));
        preview.setTimezoneSnapshot("Asia/Ho_Chi_Minh");
        preview.setStrategy(AutoScheduleStrategy.BALANCED);
        preview.setStrategyVersion("BALANCED_V1");
        preview.setApplyMode(SchedulePreviewApplyMode.ALL_OR_NOTHING);
        preview.setStatus(SchedulePreviewStatus.GENERATING);
        preview.setSlotGranularityMinutes(15);
        preview.setTotalCandidateCount(0);
        preview.setValidCandidateCount(0);
        preview.setRejectedCandidateCount(0);
        preview.setSelectedCandidateCount(0);
        preview.setGeneratedAt(Instant.now());
        preview.setExpiresAt(Instant.now().plusSeconds(3600));
        preview.setGeneratedBy(1001L);
        preview.setGenerateIdempotencyKey(genKey);
        preview.setApplyIdempotencyKey(applyKey);
        preview.setRequestFingerprint("0".repeat(64));
        return preview;
    }

    @Test
    void PREVIEW_REPO_001_persistPreview() {
        ShowtimeSchedulePreview preview = createValidPreview(UUID.randomUUID().toString(), "gen-1", null);
        previewRepository.saveAndFlush(preview);
        entityManager.clear();
        ShowtimeSchedulePreview saved = previewRepository.findByPublicId(preview.getPublicId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getVersion()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void PREVIEW_REPO_002_findByPublicId() {
        String publicId = UUID.randomUUID().toString();
        ShowtimeSchedulePreview preview = createValidPreview(publicId, "gen-2", null);
        previewRepository.saveAndFlush(preview);
        entityManager.clear();

        Optional<ShowtimeSchedulePreview> found = previewRepository.findByPublicId(publicId);
        assertThat(found).isPresent();
        assertThat(found.get().getPublicId()).isEqualTo(publicId);
    }

    @Test
    void PREVIEW_REPO_003_findByGenerateIdempotencyKey() {
        ShowtimeSchedulePreview preview = createValidPreview(UUID.randomUUID().toString(), "gen-3", null);
        previewRepository.saveAndFlush(preview);
        entityManager.clear();

        Optional<ShowtimeSchedulePreview> found = previewRepository.findByGenerateIdempotencyKey("gen-3");
        assertThat(found).isPresent();
    }

    @Test
    void PREVIEW_REPO_004_generateIdempotencyUniqueness() {
        ShowtimeSchedulePreview previewA = createValidPreview(UUID.randomUUID().toString(), "gen-dup", null);
        previewRepository.saveAndFlush(previewA);

        ShowtimeSchedulePreview previewB = createValidPreview(UUID.randomUUID().toString(), "gen-dup", null);
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            previewRepository.saveAndFlush(previewB);
        });
    }

    @Test
    void PREVIEW_REPO_005_applyIdempotencyUniqueness() {
        ShowtimeSchedulePreview previewC = createValidPreview(UUID.randomUUID().toString(), "gen-6", null);
        previewRepository.saveAndFlush(previewC);
        
        ShowtimeSchedulePreview previewD = createValidPreview(UUID.randomUUID().toString(), "gen-7", null);
        previewRepository.saveAndFlush(previewD);

        ShowtimeSchedulePreview previewA = createValidPreview(UUID.randomUUID().toString(), "gen-4", "apply-dup");
        previewRepository.saveAndFlush(previewA);

        ShowtimeSchedulePreview previewB = createValidPreview(UUID.randomUUID().toString(), "gen-5", "apply-dup");
        
        assertThrows(DataIntegrityViolationException.class, () -> {
            previewRepository.saveAndFlush(previewB);
        });
    }

    @Test
    void PREVIEW_REPO_006_optimisticVersionIncrements() {
        ShowtimeSchedulePreview preview = createValidPreview(UUID.randomUUID().toString(), "gen-8", null);
        ShowtimeSchedulePreview saved = previewRepository.saveAndFlush(preview);
        
        Long initialVersion = saved.getVersion();
        
        saved.setTotalCandidateCount(10);
        saved.setValidCandidateCount(7);
        saved.setRejectedCandidateCount(3);
        saved.setSelectedCandidateCount(5);
        ShowtimeSchedulePreview updated = previewRepository.saveAndFlush(saved);
        
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void PREVIEW_REPO_007_optimisticStaleUpdate() {
        ShowtimeSchedulePreview preview = createValidPreview(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
        previewRepository.saveAndFlush(preview);
        
        org.springframework.transaction.support.TransactionTemplate txTemplate = 
            new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        ShowtimeSchedulePreview t1 = txTemplate.execute(status -> previewRepository.findById(preview.getId()).orElseThrow());
        
        txTemplate.execute(status -> {
            ShowtimeSchedulePreview t2 = previewRepository.findById(preview.getId()).orElseThrow();
            t2.setTotalCandidateCount(20);
            t2.setValidCandidateCount(15);
            t2.setRejectedCandidateCount(5);
            t2.setSelectedCandidateCount(10);
            return previewRepository.saveAndFlush(t2);
        });

        assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
            txTemplate.execute(status -> {
                t1.setTotalCandidateCount(10);
                t1.setValidCandidateCount(7);
                t1.setRejectedCandidateCount(3);
                t1.setSelectedCandidateCount(5);
                return previewRepository.saveAndFlush(t1);
            });
        });
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void PREVIEW_REPO_008_pessimisticLockQuery() throws InterruptedException {
        ShowtimeSchedulePreview preview = createValidPreview(UUID.randomUUID().toString(), UUID.randomUUID().toString(), null);
        previewRepository.saveAndFlush(preview);
        
        org.springframework.transaction.support.TransactionTemplate txTemplate = 
            new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        java.util.concurrent.CountDownLatch lockAcquired = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch updateFinished = new java.util.concurrent.CountDownLatch(1);

        Thread thread1 = new Thread(() -> {
            txTemplate.execute(status -> {
                previewRepository.findByPublicIdForUpdate(preview.getPublicId());
                lockAcquired.countDown();
                try {
                    updateFinished.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        });
        
        thread1.setDaemon(true);
        thread1.start();
        lockAcquired.await();

        try {
            assertThrows(org.springframework.dao.PessimisticLockingFailureException.class, () -> {
                txTemplate.execute(status -> {
                    return previewRepository.findByPublicIdForUpdate(preview.getPublicId());
                });
            });
        } finally {
            updateFinished.countDown();
            thread1.join();
        }
    }

    @Test
    void PREVIEW_REPO_009_expiryQuery() {
        ShowtimeSchedulePreview expired = createValidPreview(UUID.randomUUID().toString(), "gen-9", null);
        expired.setStatus(SchedulePreviewStatus.PREVIEWED);
        expired.setExpiresAt(Instant.now().minusSeconds(10));
        previewRepository.saveAndFlush(expired);

        ShowtimeSchedulePreview active = createValidPreview(UUID.randomUUID().toString(), "gen-10", null);
        active.setStatus(SchedulePreviewStatus.PREVIEWED);
        active.setExpiresAt(Instant.now().plusSeconds(3600));
        previewRepository.saveAndFlush(active);

        List<ShowtimeSchedulePreview> found = previewRepository.findByStatusAndExpiresAtLessThanEqual(
                SchedulePreviewStatus.PREVIEWED, Instant.now());
                
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getId()).isEqualTo(expired.getId());
    }
    
    @Test
    void PREVIEW_REPO_010_cinemaRelationLazy() {
        ShowtimeSchedulePreview preview = createValidPreview(UUID.randomUUID().toString(), "gen-11", null);
        previewRepository.saveAndFlush(preview);
        entityManager.clear();

        Optional<ShowtimeSchedulePreview> found = previewRepository.findById(preview.getId());
        assertThat(found).isPresent();
        assertThat(org.hibernate.Hibernate.isInitialized(found.get().getCinema())).isFalse();
    }
}
