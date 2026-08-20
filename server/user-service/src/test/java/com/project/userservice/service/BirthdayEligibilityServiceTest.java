package com.project.userservice.service;

import com.project.userservice.entity.User;
import com.project.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirthdayEligibilityServiceTest {
    @Mock UserRepository repository;

    @Test
    void mapsFeb29BirthdaysToFeb28InNonLeapYearsWithoutExposingFullProfile() {
        User user = new User();
        user.setAccountId(42L);
        user.setBirthday(LocalDate.of(2000, 2, 29));
        when(repository.findBirthdayEligible(eq(2), eq(28), eq(true), any(Pageable.class)))
                .thenReturn(List.of(user));

        var result = new BirthdayEligibilityService(repository)
                .findEligible(LocalDate.of(2026, 2, 28), 0, 500);

        assertEquals(1, result.size());
        assertEquals(42L, result.getFirst().customerId());
        assertEquals(2, result.getFirst().birthdayMonth());
        assertEquals(29, result.getFirst().birthdayDay());
        verify(repository).findBirthdayEligible(eq(2), eq(28), eq(true), any(Pageable.class));
    }
}
