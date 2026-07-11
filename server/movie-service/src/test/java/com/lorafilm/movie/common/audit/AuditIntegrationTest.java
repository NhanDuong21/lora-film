package com.lorafilm.movie.common.audit;

import com.lorafilm.movie.common.config.AuditConfig;
import com.lorafilm.movie.common.config.AuditorAwareImpl;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(AuditConfig.class)
@ActiveProfiles("test")
public class AuditIntegrationTest {

    @Autowired
    private TestEntityRepository repository;

    @BeforeEach
    public void setup() {
        // Mock the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("12345", null, Collections.emptyList())
        );
    }

    @Test
    public void testCreateAudit() {
        TestEntity entity = new TestEntity();
        entity.setName("Test Create");
        
        TestEntity savedEntity = repository.saveAndFlush(entity);

        assertNotNull(savedEntity.getId());
        assertNotNull(savedEntity.getCreatedAt());
        assertNotNull(savedEntity.getUpdatedAt());
        assertEquals(12345L, savedEntity.getCreatedBy());
        assertEquals(12345L, savedEntity.getUpdatedBy());

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            System.out.println("JSON Serialization Check: " + mapper.writeValueAsString(savedEntity));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testUpdateAudit() {
        TestEntity entity = new TestEntity();
        entity.setName("Test Init");
        
        TestEntity savedEntity = repository.saveAndFlush(entity);
        Instant originalCreatedAt = savedEntity.getCreatedAt();
        Instant originalUpdatedAt = savedEntity.getUpdatedAt();

        // Simulate another user updating
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("99999", null, Collections.emptyList())
        );

        savedEntity.setName("Test Update");
        TestEntity updatedEntity = repository.saveAndFlush(savedEntity);

        assertEquals(originalCreatedAt, updatedEntity.getCreatedAt());
        assertTrue(updatedEntity.getUpdatedAt().isAfter(originalUpdatedAt) || updatedEntity.getUpdatedAt().equals(originalUpdatedAt));
        assertEquals(12345L, updatedEntity.getCreatedBy());
        assertEquals(99999L, updatedEntity.getUpdatedBy());
    }
}

@Entity
class TestEntity extends BaseAuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

@Repository
interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
}
