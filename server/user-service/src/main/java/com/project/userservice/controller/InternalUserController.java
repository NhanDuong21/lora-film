package com.project.userservice.controller;

import com.project.userservice.dto.response.ApiResponse;
import com.project.userservice.dto.response.NotificationRecipientResponse;
import com.project.userservice.service.NotificationRecipientService;
import com.project.userservice.service.BirthdayEligibilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final NotificationRecipientService recipientService;
    private final BirthdayEligibilityService birthdayEligibilityService;

    public InternalUserController(
            NotificationRecipientService recipientService,
            BirthdayEligibilityService birthdayEligibilityService) {
        this.recipientService = recipientService;
        this.birthdayEligibilityService = birthdayEligibilityService;
    }

    @GetMapping("/{accountId}/notification-recipient")
    public ResponseEntity<ApiResponse<NotificationRecipientResponse>> notificationRecipient(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Notification recipient retrieved",
                recipientService.findByAccountId(accountId)));
    }

    @PostMapping("/validate-active")
    public ResponseEntity<ApiResponse<List<Long>>> validateActiveUsers(
            @Valid @RequestBody ActiveUserValidationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Active users validated",
                recipientService.findActiveAccountIds(
                        request.accountIds(), Boolean.TRUE.equals(request.testAccountsOnly()))));
    }

    @GetMapping("/birthday-eligible")
    public ResponseEntity<ApiResponse<List<BirthdayEligibilityService.BirthdayEligibleUser>>>
            birthdayEligible(
                    @RequestParam LocalDate date,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "500") int size,
                    @RequestParam(defaultValue = "false") boolean testAccountsOnly) {
        int safeSize = Math.max(1, Math.min(size, 500));
        return ResponseEntity.ok(ApiResponse.success(
                "Birthday eligible users retrieved",
                birthdayEligibilityService.findEligible(
                        date, Math.max(page, 0), safeSize, testAccountsOnly)));
    }

    public record ActiveUserValidationRequest(
            @NotEmpty @Size(max = 1000)
            List<@Positive Long> accountIds,
            Boolean testAccountsOnly) {
    }
}
