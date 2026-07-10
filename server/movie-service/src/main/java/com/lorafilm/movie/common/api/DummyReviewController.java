package com.lorafilm.movie.common.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
public class DummyReviewController {

    public static class DummyDto {
        @NotBlank(message = "Name cannot be blank")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @GetMapping("/api/internal/test")
    public ApiResponse<String> internalTest() {
        return ApiResponse.ok("Internal Area");
    }

    @PostMapping("/api/public/validation")
    public ApiResponse<String> publicValidation(@Valid @RequestBody DummyDto dto) {
        return ApiResponse.ok("Valid");
    }
}
