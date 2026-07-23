package com.lorafilm.booking.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonLayout extends LayoutBase<ILoggingEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("timestamp", formatter.format(Instant.ofEpochMilli(event.getTimeStamp())));
        logMap.put("service", "booking-service");
        logMap.put("level", event.getLevel().toString());

        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            String traceId = mdc.get("traceId");
            if (traceId != null && !traceId.isEmpty()) {
                logMap.put("traceId", traceId);
            }
            String correlationId = mdc.get("correlationId");
            if (correlationId != null && !correlationId.isEmpty()) {
                logMap.put("correlationId", correlationId);
            }
            String userId = mdc.get("userId");
            if (userId != null && !userId.isEmpty()) {
                try {
                    logMap.put("userId", Long.parseLong(userId));
                } catch (NumberFormatException e) {
                    logMap.put("userId", userId);
                }
            }
            String bookingId = mdc.get("bookingId");
            if (bookingId != null && !bookingId.isEmpty()) {
                try {
                    logMap.put("bookingId", Long.parseLong(bookingId));
                } catch (NumberFormatException e) {
                    logMap.put("bookingId", bookingId);
                }
            }
            String action = mdc.get("action");
            if (action != null && !action.isEmpty()) {
                logMap.put("action", action);
            }
        }

        logMap.put("message", event.getFormattedMessage());

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy != null) {
            logMap.put("exceptionType", throwableProxy.getClassName());
            logMap.put("exceptionMessage", throwableProxy.getMessage());
            
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElementProxy step : throwableProxy.getStackTraceElementProxyArray()) {
                stackTrace.append(step.toString()).append("\n");
            }
            logMap.put("stackTrace", stackTrace.toString());
        }

        try {
            return objectMapper.writeValueAsString(logMap) + "\n";
        } catch (Exception e) {
            return "{\"level\":\"ERROR\",\"message\":\"Failed to format log as JSON: " + e.getMessage() + "\"}\n";
        }
    }
}
