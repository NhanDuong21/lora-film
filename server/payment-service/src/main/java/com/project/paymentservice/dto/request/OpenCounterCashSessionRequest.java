package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OpenCounterCashSessionRequest(
        @NotNull(message = "Vui lòng nhập tiền đầu ca")
        @DecimalMin(value = "0.00", message = "Tiền đầu ca không được âm")
        BigDecimal openingFloat,
        @Size(max = 500, message = "Ghi chú đầu ca tối đa 500 ký tự")
        String note) {
}
