package com.project.scoreservice.controller;
 
import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.MembershipTierResponse;
import com.project.scoreservice.service.MembershipTierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/membership-tiers")
@Tag(name = "Membership Tier Public", description = "Public endpoints for membership tiers")
public class MembershipTierController {
 
    private final MembershipTierService membershipTierService;
 
    public MembershipTierController(MembershipTierService membershipTierService) {
        this.membershipTierService = membershipTierService;
    }
 
    @GetMapping
    @Operation(summary = "Get all membership tiers", description = "Retrieve list of all active membership tiers sorted by minimum points ascending")
    public ResponseEntity<ApiResponse<List<MembershipTierResponse>>> getMembershipTiers() {
        List<MembershipTierResponse> response = membershipTierService.getMembershipTiers();
        return ResponseEntity.ok(ApiResponse.success("Membership tiers retrieved successfully", response));
    }
}
