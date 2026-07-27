package com.project.userservice.service;

import com.project.userservice.entity.Avatar;
import com.project.userservice.entity.User;
import com.project.userservice.exception.BusinessException;
import com.project.userservice.repository.AvatarRepository;
import com.project.userservice.repository.UserRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.List;

@Service
public class AvatarService {
    private final UserRepository userRepository;
    private final AvatarRepository avatarRepository;
    private final UserAuditService auditService;
    private final UserDomainEventService eventService;
    private final SecureFileStorageService fileStorageService;

    public AvatarService(UserRepository userRepository, AvatarRepository avatarRepository,
                         UserAuditService auditService, UserDomainEventService eventService,
                         SecureFileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.avatarRepository = avatarRepository;
        this.auditService = auditService;
        this.eventService = eventService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public String upload(Long accountId, MultipartFile file) {
        User user = findUser(accountId);
        List<Avatar> previousAvatars = avatarRepository.findByAccountIdOrderByUploadedAtDesc(accountId);
        SecureFileStorageService.StoredFile storedFile = fileStorageService.storeAvatar(file);
        try {
            Avatar avatar = new Avatar();
            avatar.setAccountId(accountId);
            avatar.setFileName(storedFile.fileName());
            avatar.setFileUrl("/api/users/profile/avatar/files/" + storedFile.fileName());
            avatar.setContentType(storedFile.contentType());
            avatar.setFileSize(storedFile.fileSize());
            avatarRepository.save(avatar);
            user.setAvatarUrl(avatar.getFileUrl());
            userRepository.save(user);
            avatarRepository.deleteAll(previousAvatars);
            previousAvatars.forEach(previous -> fileStorageService.deleteAfterCommit(
                    "avatars", previous.getFileName()));
            auditService.log("AVATAR_UPDATED", "USER", accountId, null);
            eventService.record("CUSTOMER_UPDATED", "USER", accountId,
                    Map.of("accountId", accountId, "avatarUrl", avatar.getFileUrl()));
            return avatar.getFileUrl();
        } catch (RuntimeException exception) {
            try {
                fileStorageService.delete("avatars", storedFile.fileName());
            } catch (RuntimeException ignored) {
                // Preserve the database or business failure that prevented metadata persistence.
            }
            throw exception;
        }
    }

    @Transactional
    public void delete(Long accountId) {
        User user = findUser(accountId);
        List<Avatar> avatars = avatarRepository.findByAccountIdOrderByUploadedAtDesc(accountId);
        user.setAvatarUrl(null);
        userRepository.save(user);
        avatarRepository.deleteAll(avatars);
        avatars.forEach(avatar -> fileStorageService.deleteAfterCommit("avatars", avatar.getFileName()));
        auditService.log("AVATAR_DELETED", "USER", accountId, null);
    }

    @Transactional(readOnly = true)
    public Resource load(String fileName) {
        return fileStorageService.load("avatars", fileName);
    }

    private User findUser(Long accountId) {
        return userRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("User not found", "USER_001"));
    }
}
