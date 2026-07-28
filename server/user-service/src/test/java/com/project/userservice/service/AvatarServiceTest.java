package com.project.userservice.service;

import com.project.userservice.entity.Avatar;
import com.project.userservice.entity.User;
import com.project.userservice.repository.AvatarRepository;
import com.project.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AvatarRepository avatarRepository;
    @Mock
    private UserAuditService auditService;
    @Mock
    private UserDomainEventService eventService;
    @Mock
    private SecureFileStorageService fileStorageService;
    @Mock
    private MultipartFile multipartFile;

    private AvatarService avatarService;
    private User user;
    private Avatar previousAvatar;

    @BeforeEach
    void setUp() {
        avatarService = new AvatarService(
                userRepository,
                avatarRepository,
                auditService,
                eventService,
                fileStorageService);
        user = new User();
        user.setAccountId(18L);
        user.setAvatarUrl("/api/users/profile/avatar/files/old.png");
        previousAvatar = new Avatar();
        previousAvatar.setAccountId(18L);
        previousAvatar.setFileName("old.png");
        previousAvatar.setFileUrl(user.getAvatarUrl());
        previousAvatar.setContentType("image/png");
        previousAvatar.setFileSize(100L);
        when(userRepository.findById(18L)).thenReturn(Optional.of(user));
        when(avatarRepository.findByAccountIdOrderByUploadedAtDesc(18L))
                .thenReturn(List.of(previousAvatar));
    }

    @Test
    void uploadReplacesMetadataAndSchedulesOldFileDeletionAfterCommit() {
        when(fileStorageService.storeAvatar(multipartFile))
                .thenReturn(new SecureFileStorageService.StoredFile(
                        "new.png", "image/png", 200L));

        String avatarUrl = avatarService.upload(18L, multipartFile);

        assertEquals("/api/users/profile/avatar/files/new.png", avatarUrl);
        assertEquals(avatarUrl, user.getAvatarUrl());
        verify(avatarRepository).deleteAll(List.of(previousAvatar));
        verify(fileStorageService).deleteAfterCommit("avatars", "old.png");
    }

    @Test
    void deleteRemovesMetadataAndSchedulesEveryStoredFileForDeletion() {
        avatarService.delete(18L);

        assertNull(user.getAvatarUrl());
        verify(avatarRepository).deleteAll(List.of(previousAvatar));
        verify(fileStorageService).deleteAfterCommit("avatars", "old.png");
    }
}
