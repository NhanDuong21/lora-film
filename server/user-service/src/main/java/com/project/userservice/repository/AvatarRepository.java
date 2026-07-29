package com.project.userservice.repository;

import com.project.userservice.entity.Avatar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvatarRepository extends JpaRepository<Avatar, Long> {
    List<Avatar> findByAccountIdOrderByUploadedAtDesc(Long accountId);
    Optional<Avatar> findFirstByFileName(String fileName);
}
