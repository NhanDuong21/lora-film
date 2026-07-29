package com.project.promotionservice.common.constant;

public final class ValidationConstants {

    public static final String UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    /**
     * Cross-service customer identity. Auth, User and Score currently expose a
     * numeric account ID, while older Promotion records may contain UUIDs.
     */
    public static final String USER_REFERENCE_PATTERN =
            "^(?:[1-9][0-9]{0,18}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$";
    public static final String CONFIG_KEY_PATTERN = "^[A-Za-z][A-Za-z0-9_.-]{1,149}$";

    private ValidationConstants() {
    }
}
