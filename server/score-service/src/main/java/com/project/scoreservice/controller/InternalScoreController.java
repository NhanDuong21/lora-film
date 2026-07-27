package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.*;
import com.project.scoreservice.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/internal/scores", "/internal"})
@Tag(name = "Score Internal Integration", description = "Internal service integration endpoints")
public class InternalScoreController {

    private final ScoreService scoreService;

    public InternalScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user score balance internally", description = "Internal endpoint to quickly fetch current score details and earning rate for a user")
    public ResponseEntity<ApiResponse<InternalUserScoreResponse>> getUserScore(@PathVariable Long userId) {
        InternalUserScoreResponse response = scoreService.getInternalUserScore(userId);
        return ResponseEntity.ok(ApiResponse.success("User score retrieved successfully", response));
    }

    @PostMapping("/earn")
    @Operation(summary = "Earn points from booking", description = "Award points to customer after booking completion")
    public ResponseEntity<ApiResponse<ScoreEarnResponse>> earnPoints(@RequestBody ScoreEarnRequest request) {
        ScoreEarnResponse response = scoreService.earnPoints(request);
        return ResponseEntity.ok(ApiResponse.success("Points earned successfully", response));
    }

    @PostMapping({"/hold", "/redeem/hold", "/scores/hold", "/scores/redeem/hold"})
    @Operation(summary = "Hold points for redemption", description = "Temporarily reserve points for a pending booking")
    public ResponseEntity<ApiResponse<ScoreHoldResponse>> holdPoints(@RequestBody ScoreHoldRequest request) {
        ScoreHoldResponse response = scoreService.holdPoints(request);
        return ResponseEntity.ok(ApiResponse.success("Points held successfully", response));
    }

    @PostMapping({"/commit", "/redeem/commit", "/scores/commit", "/scores/redeem/commit"})
    @Operation(summary = "Commit held points", description = "Confirm and deduct previously held points upon booking confirmation")
    public ResponseEntity<ApiResponse<ScoreCommitResponse>> commitPoints(@RequestBody ScoreCommitRequest request) {
        ScoreCommitResponse response = scoreService.commitPoints(request);
        return ResponseEntity.ok(ApiResponse.success("Points committed successfully", response));
    }

    @PostMapping({"/release", "/redeem/release", "/scores/release", "/scores/redeem/release"})
    @Operation(summary = "Release held points", description = "Release previously held points back to customer available balance upon booking cancellation")
    public ResponseEntity<ApiResponse<ScoreReleaseResponse>> releasePoints(@RequestBody ScoreReleaseRequest request) {
        ScoreReleaseResponse response = scoreService.releasePoints(request);
        return ResponseEntity.ok(ApiResponse.success("Points released successfully", response));
    }

    @PostMapping({"/redeem", "/scores/redeem"})
    @Operation(summary = "Redeem points directly", description = "Directly redeem points for a booking without hold")
    public ResponseEntity<ApiResponse<ScoreRedeemResponse>> redeemPoints(@RequestBody ScoreRedeemRequest request) {
        ScoreRedeemResponse response = scoreService.redeemPoints(request);
        String msg = Boolean.TRUE.equals(response.idempotent()) ? "Score redeem event was already processed" : "Score redeemed successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, response));
    }

    @PostMapping({"/refund-redeem", "/scores/refund-redeem"})
    @Operation(summary = "Refund redeemed points", description = "Refund points that were previously redeemed for a booking")
    public ResponseEntity<ApiResponse<ScoreRefundResponse>> refundRedeem(@RequestBody ScoreRefundRequest request) {
        ScoreRefundResponse response = scoreService.refundRedeem(request);
        return ResponseEntity.ok(ApiResponse.success("Redeemed score refunded successfully", response));
    }

    @PostMapping({"/revoke-earn", "/scores/revoke-earn"})
    @Operation(summary = "Revoke earned points", description = "Revoke points previously awarded for an earn transaction")
    public ResponseEntity<ApiResponse<ScoreRevokeResponse>> revokeEarn(@RequestBody ScoreRevokeRequest request) {
        ScoreRevokeResponse response = scoreService.revokeEarn(request);
        org.springframework.http.HttpStatus status = Boolean.TRUE.equals(response.idempotent()) ? org.springframework.http.HttpStatus.OK : org.springframework.http.HttpStatus.CREATED;
        return ResponseEntity.status(status).body(ApiResponse.success("Points revoked successfully", response));
    }
}


