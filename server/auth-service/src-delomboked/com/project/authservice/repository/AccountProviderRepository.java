package com.project.authservice.repository;

import com.project.authservice.entity.AccountProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountProviderRepository extends JpaRepository<AccountProvider, Long> {
    Optional<AccountProvider> findByProviderAndProviderUserId(String provider, String providerUserId);
    boolean existsByAccountIdAndProvider(Long accountId, String provider);
}
