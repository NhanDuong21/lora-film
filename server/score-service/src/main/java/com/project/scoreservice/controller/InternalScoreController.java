package com.project.scoreservice.controller;
 
import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.InternalUserScoreResponse;
import com.project.scoreservice.dto.ScoreEarnRequest;
import com.project.scoreservice.dto.ScoreEarnResponse;
import com.project.scoreservice.dto.ScoreRedeemRequest;
import com.project.scoreservice.dto.ScoreRedeemResponse;
import com.project.scoreservice.dto.ScoreRefundRequest;
import com.project.scoreservice.dto.ScoreRefundResponse;
import com.project.scoreservice.dto.ScoreRevokeRequest;
import com.project.scoreservice.dto.ScoreRevokeResponse;
import com.project.scoreservice.dto.UserScoreResponse;
import com.project.scoreservice.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/scores")
@Tag(name = "Score Internal Integration", description = "Internal service integration endpoints (used by Booking, Payment, and upstream systems)")
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
    @Operation(summary = "Earn score internally", description = "Internal endpoint to award score points to a user based on booking payment")
    public ResponseEntity<ApiResponse<ScoreEarnResponse>> earnScore(@Valid @RequestBody ScoreEarnRequest request) {
        ScoreEarnResponse response = scoreService.earnScore(request);
        String message = Boolean.TRUE.equals(response.getIdempotent())
                ? "Transaction already processed successfully"
                : "Score earned successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/redeem")
    @Operation(summary = "Redeem score internally", description = "Internal endpoint to redeem score points for a booking purchase")
    public ResponseEntity<ApiResponse<ScoreRedeemResponse>> redeemScore(@Valid @RequestBody ScoreRedeemRequest request) {
        ScoreRedeemResponse response = scoreService.redeemScore(request);
        String message = Boolean.TRUE.equals(response.getIdempotent())
                ? "Score redeem event was already processed"
                : "Score redeemed successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/refund-redeem")
    @Operation(summary = "Refund redeem score internally", description = "Internal endpoint to refund redeemed score points for a cancelled/expired booking")
    public ResponseEntity<ApiResponse<ScoreRefundResponse>> refundRedeem(@Valid @RequestBody ScoreRefundRequest request) {
        ScoreRefundResponse response = scoreService.refundRedeem(request);
        String message = Boolean.TRUE.equals(response.getIdempotent())
                ? "Redeemed score refund was already processed"
                : "Redeemed score refunded successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/revoke-earn")
    @Operation(summary = "Revoke earned score internally", description = "Internal endpoint to revoke earned score points for a refunded booking")
    public ResponseEntity<ApiResponse<ScoreRevokeResponse>> revokeEarn(@Valid @RequestBody ScoreRevokeRequest request) {
        ScoreRevokeResponse response = scoreService.revokeEarn(request);
        if (Boolean.TRUE.equals(response.getIdempotent())) {
            return ResponseEntity.ok(ApiResponse.success("Earned score revoke was already processed", response));
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Earned score revoked successfully", response));
    }
}
