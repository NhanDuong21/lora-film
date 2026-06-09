package com.project.authservice.repository;

import java.util.Optional;

import com.project.authservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

	boolean existsByEmail(String email);

	Optional<Account> findByEmail(String email);
}