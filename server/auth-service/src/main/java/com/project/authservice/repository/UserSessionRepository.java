package com.project.authservice.repository;

import com.project.authservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    
    List<UserSession> findByAccountIdAndIsActiveTrue(Long accountId);
    
    Optional<UserSession> findByIdAndAccountId(String id, Long accountId);
    
    Optional<UserSession> findByAccessTokenHash(String accessTokenHash);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.id = :sessionId")
    void revokeSession(String sessionId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.account.id = :accountId AND s.id != :excludeSessionId")
    void revokeAllExcept(Long accountId, String excludeSessionId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.account.id = :accountId")
    void revokeAllForAccount(Long accountId);
}
