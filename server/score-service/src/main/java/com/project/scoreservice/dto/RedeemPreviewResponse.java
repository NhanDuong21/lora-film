package com.project.scoreservice.dto;

import java.math.BigDecimal;

public record RedeemPreviewResponse(
    boolean eligible,
    int requestedPoints,
    int availablePoints,
    BigDecimal discountAmount,
    BigDecimal valuePerPoint,
    String message
) {}
