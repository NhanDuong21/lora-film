package com.project.scoreservice.dto;

import java.math.BigDecimal;

public record RedeemPreviewResponse(
    boolean eligible,
    int requestedPoints,
    int availablePoints,
    int maxRedeemablePoints,
    BigDecimal discountAmount,
    BigDecimal bookingAmount,
    BigDecimal remainingAmount,
    BigDecimal valuePerPoint,
    String message
) {}
