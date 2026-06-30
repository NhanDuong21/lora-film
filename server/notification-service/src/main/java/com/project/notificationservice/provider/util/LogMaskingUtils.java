package com.project.notificationservice.provider.util;

public final class LogMaskingUtils {
    private LogMaskingUtils() {}

    public static String maskRecipient(String recipient) {
        if (recipient == null || recipient.isEmpty()) {
            return "null";
        }
        int atIndex = recipient.indexOf('@');
        if (atIndex <= 0) {
            // Treat as non-email (e.g. phone number)
            if (recipient.length() <= 4) {
                return "***";
            }
            return recipient.substring(0, 2) + "***" + recipient.substring(recipient.length() - 2);
        }
        char firstChar = recipient.charAt(0);
        String domain = recipient.substring(atIndex);
        return firstChar + "***" + domain;
    }
}
