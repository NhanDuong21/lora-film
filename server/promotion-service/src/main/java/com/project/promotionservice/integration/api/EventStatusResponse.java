package com.project.promotionservice.integration.api;

import java.util.Map;

public record EventStatusResponse(Map<String, Long> outbox, Map<String, Long> inbox) {}
