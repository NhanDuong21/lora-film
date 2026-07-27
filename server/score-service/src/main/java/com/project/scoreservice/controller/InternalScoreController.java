package com.project.scoreservice.controller;

import com.project.scoreservice.common.ApiResponse;
import com.project.scoreservice.dto.InternalUserScoreResponse;
import com.project.scoreservice.service.ScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/scores")
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
}
