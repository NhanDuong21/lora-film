package com.project.userservice.service;

import com.project.userservice.entity.User;
import com.project.userservice.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
public class BirthdayEligibilityService {
    private final UserRepository userRepository;

    public BirthdayEligibilityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<BirthdayEligibleUser> findEligible(
            LocalDate executionDate, int page, int size) {
        return findEligible(executionDate, page, size, false);
    }

    public List<BirthdayEligibleUser> findEligible(
            LocalDate executionDate, int page, int size, boolean testAccountsOnly) {
        boolean includeLeapDay = executionDate.getMonthValue() == 2
                && executionDate.getDayOfMonth() == 28
                && !Year.isLeap(executionDate.getYear());
        List<User> users = testAccountsOnly
                ? userRepository.findTestBirthdayEligible(
                    executionDate.getMonthValue(), executionDate.getDayOfMonth(),
                    includeLeapDay, PageRequest.of(page, size))
                : userRepository.findBirthdayEligible(
                    executionDate.getMonthValue(), executionDate.getDayOfMonth(),
                    includeLeapDay, PageRequest.of(page, size));
        return users
                .stream()
                .map(user -> new BirthdayEligibleUser(
                        user.getAccountId(), user.getBirthday().getMonthValue(),
                        user.getBirthday().getDayOfMonth(), "ACTIVE", "ELIGIBLE",
                        Boolean.TRUE.equals(user.getTestAccount())))
                .toList();
    }

    public record BirthdayEligibleUser(
            Long customerId,
            int birthdayMonth,
            int birthdayDay,
            String accountStatus,
            String memberStatus,
            boolean testAccount) {
    }
}
