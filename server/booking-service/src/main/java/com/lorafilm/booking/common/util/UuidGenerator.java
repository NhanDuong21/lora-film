package com.lorafilm.booking.common.util;

import java.util.UUID;

public final class UuidGenerator {

    private UuidGenerator() {
    }

    public static UUID generateUuid() {
        return UUID.randomUUID();
    }
}
