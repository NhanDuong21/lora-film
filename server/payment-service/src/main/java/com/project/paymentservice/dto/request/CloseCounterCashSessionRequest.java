package com.project.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CloseCounterCashSessionRequest(
        @NotNull(message = "Vui lòng nhập tiền đã kiểm đếm")
        @DecimalMin(value = "0.00", message = "Tiền kiểm đếm không được âm")
        BigDecimal countedCash,
        @Size(max = 1000, message = "Ghi chú bàn giao tối đa 1.000 ký tự")
        String note) {
}
