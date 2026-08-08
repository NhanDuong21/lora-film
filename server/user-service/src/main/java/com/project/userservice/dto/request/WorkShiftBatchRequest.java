package com.project.userservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WorkShiftBatchRequest(
        @NotNull Long employeeId,
        @NotEmpty @Size(max = 8) List<@Valid WorkShiftPeriodRequest> periods,
        @NotBlank @Size(max = 150) String location,
        @Size(max = 500) String note
) {
}
