package com.project.authservice.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileCreatedEventData {
    private Long accountId;
    private String requestId;
    private String createdAt;
}
