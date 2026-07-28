package com.project.userservice.service;

import com.project.userservice.dto.response.UserProfileResponse;
import com.project.userservice.entity.User;
import com.project.userservice.repository.UserRepository;
import com.project.userservice.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAuditService auditService;
    @Mock
    private UserDomainEventService eventService;
    @Mock
    private com.project.userservice.repository.CustomerProfileRepository customerProfileRepository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository, auditService, eventService, customerProfileRepository);
    }

    @Test
    void getUserProfile_exposesOperationalEmailAndCustomerCode() {
        User user = user(5L, "Minh Duy", "duy@example.com", "0900000001");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getUserProfile(5L);

        assertEquals("CUS0000000005", response.getCustomerCode());
        assertEquals("duy@example.com", response.getEmail());
        assertEquals("Minh Duy", response.getFullName());
    }

    @Test
    void searchUserProfiles_acceptsCustomerCodeAndTextFields() {
        User byCode = user(5L, "Minh Duy", "duy@example.com", "0900000001");
        User byText = user(8L, "Nguyễn An", "an@example.com", "0900000008");
        when(userRepository.findById(5L)).thenReturn(Optional.of(byCode));
        when(userRepository.searchOperationalProfiles(
                eq("%kh000005%"), any(Pageable.class)))
                .thenReturn(List.of(byCode, byText));

        List<UserProfileResponse> response =
                userService.searchUserProfiles("KH000005", 20);

        assertEquals(List.of("CUS0000000005", "CUS0000000008"),
                response.stream().map(UserProfileResponse::getCustomerCode).toList());
        verify(customerProfileRepository).findByAccountIdIn(List.of(5L, 8L));
        verify(customerProfileRepository, never()).findByAccountId(any());
    }

    private User user(Long id, String fullName, String email, String phone) {
        User user = new User();
        user.setAccountId(id);
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phone);
        return user;
    }
}
