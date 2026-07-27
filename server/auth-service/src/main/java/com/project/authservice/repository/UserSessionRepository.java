package com.project.authservice.repository;

import com.project.authservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    List<UserSession> findByAccountIdAndIsOnlineTrue(Long accountId);
    
    Optional<UserSession> findByIdAndAccountId(Long id, Long accountId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isOnline = false WHERE s.id = :sessionId")
    void revokeSession(Long sessionId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isOnline = false WHERE s.account.id = :accountId AND s.id != :excludeSessionId")
    void revokeAllExcept(Long accountId, Long excludeSessionId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isOnline = false WHERE s.account.id = :accountId")
    void revokeAllForAccount(Long accountId);
}
