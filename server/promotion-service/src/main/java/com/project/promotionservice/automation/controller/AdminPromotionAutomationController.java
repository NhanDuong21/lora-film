package com.project.promotionservice.automation.controller;

import com.project.promotionservice.automation.dto.AutomationDtos.*;
import com.project.promotionservice.automation.entity.PromotionAutomationRun;
import com.project.promotionservice.automation.service.PromotionAutomationService;
import com.project.promotionservice.automation.service.PromotionAnomalyCaseService;
import com.project.promotionservice.common.response.ApiResponse;
import com.project.promotionservice.common.web.SecurityActor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import static com.project.promotionservice.common.constant.ValidationConstants.UUID_PATTERN;

@RestController
@Validated
@RequestMapping("/api/admin")
public class AdminPromotionAutomationController {
    private final PromotionAutomationService service;
    private final PromotionAnomalyCaseService anomalyCaseService;

    public AdminPromotionAutomationController(
            PromotionAutomationService service,
            PromotionAnomalyCaseService anomalyCaseService) {
        this.service = service;
        this.anomalyCaseService = anomalyCaseService;
    }

    @GetMapping("/promotion-playbooks")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<List<PlaybookView>>> playbooks(
            @RequestParam(defaultValue = "false") boolean includeTestData) {
        return ResponseEntity.ok(ApiResponse.success(service.playbooks(includeTestData)));
    }

    @PostMapping("/promotion-playbooks")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PlaybookView>> create(
            @Valid @RequestBody PlaybookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Playbook draft created", service.save(null, request, SecurityActor.current())));
    }

    @PutMapping("/promotion-playbooks/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PlaybookView>> update(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @Valid @RequestBody PlaybookRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Playbook draft updated", service.save(id, request, SecurityActor.current())));
    }

    @PostMapping("/promotion-playbooks/{id}/submit")
    @PreAuthorize("hasAuthority('PROMOTION_AUTHOR')")
    public ResponseEntity<ApiResponse<PlaybookView>> submit(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Playbook submitted for approval", service.submit(id, SecurityActor.current())));
    }

    @PostMapping("/promotion-playbooks/{id}/approve")
    @PreAuthorize("hasAnyAuthority('PROMOTION_APPROVE_STANDARD','PROMOTION_APPROVE_HIGH_BUDGET')")
    public ResponseEntity<ApiResponse<PlaybookView>> approve(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Playbook approved and activated", service.approve(id, SecurityActor.current())));
    }

    @PostMapping("/promotion-playbooks/{id}/pause")
    @PreAuthorize("hasAuthority('PROMOTION_OPERATE')")
    public ResponseEntity<ApiResponse<PlaybookView>> pause(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Playbook paused", service.pause(id, SecurityActor.current())));
    }

    @PostMapping("/promotion-playbooks/{id}/run")
    @PreAuthorize("hasAuthority('PROMOTION_OPERATE')")
    public ResponseEntity<ApiResponse<RunView>> runNow(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @RequestParam(required = false) LocalDate date) {
        PlaybookView playbook = service.playbooks().stream()
                .filter(item -> item.publicId().equals(id)).findFirst()
                .orElseThrow();
        if (!PromotionAutomationService.BIRTHDAY.equals(playbook.code())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ApiResponse<>(false,
                            "This event-driven playbook cannot be run manually", null));
        }
        PromotionAutomationRun created = service.createBirthdayRun(
                date == null ? LocalDate.now() : date,
                SecurityActor.current(), "ADMIN_RUN_NOW");
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("Automation run created", service.run(created.getPublicId())));
    }

    @GetMapping("/promotion-runs")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<List<RunView>>> runs(
            @RequestParam(defaultValue = "false") boolean includeTestData) {
        return ResponseEntity.ok(ApiResponse.success(service.recentRuns(includeTestData)));
    }

    @GetMapping("/promotion-runs/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<RunView>> run(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @RequestParam(defaultValue = "false") boolean includeTestData) {
        return ResponseEntity.ok(ApiResponse.success(
                service.run(id, includeTestData)));
    }

    @PostMapping("/promotion-runs/{id}/issue-jobs")
    @PreAuthorize("hasAuthority('PROMOTION_OPERATE')")
    public ResponseEntity<ApiResponse<IssueJobView>> issue(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @Valid @RequestBody(required = false) IssueJobRequest request) {
        int batchSize = request == null || request.batchSize() == null
                ? 200 : request.batchSize();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                "Issue job accepted", service.createIssueJob(id, batchSize)));
    }

    @GetMapping("/promotion-opportunities")
    @PreAuthorize("hasAuthority('PROMOTION_VIEW')")
    public ResponseEntity<ApiResponse<List<OpportunityView>>> opportunities(
            @RequestParam(defaultValue = "false") boolean includeTestData) {
        return ResponseEntity.ok(ApiResponse.success(
                service.opportunities(includeTestData)));
    }

    @GetMapping("/promotion-anomaly-cases")
    @PreAuthorize("hasAuthority('PROMOTION_AUDIT_VIEW')")
    public ResponseEntity<ApiResponse<List<AnomalyCaseView>>> anomalyCases(
            @RequestParam(defaultValue = "false") boolean includeTestData) {
        return ResponseEntity.ok(ApiResponse.success(
                anomalyCaseService.openCases(includeTestData)));
    }

    @PostMapping("/promotion-anomaly-cases/{id}/assign")
    @PreAuthorize("hasAuthority('PROMOTION_OPERATE')")
    public ResponseEntity<ApiResponse<AnomalyCaseView>> assignAnomaly(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id) {
        return ResponseEntity.ok(ApiResponse.success("Đã nhận xử lý vụ việc",
                anomalyCaseService.assign(id, SecurityActor.current())));
    }

    @PostMapping("/promotion-anomaly-cases/{id}/resolve")
    @PreAuthorize("hasAuthority('PROMOTION_OPERATE')")
    public ResponseEntity<ApiResponse<AnomalyCaseView>> resolveAnomaly(
            @PathVariable @Pattern(regexp = UUID_PATTERN) String id,
            @Valid @RequestBody ResolveAnomalyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Đã đóng vụ việc",
                anomalyCaseService.resolve(id, request, SecurityActor.current())));
    }

    public record IssueJobRequest(@Min(200) @Max(500) Integer batchSize) { }
}
