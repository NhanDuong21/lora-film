package com.project.notificationservice.provider.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LogMaskingUtilsTest {

    @Test
    public void testMaskRecipient_Email() {
        assertEquals("j***@gmail.com", LogMaskingUtils.maskRecipient("john.doe@gmail.com"));
        assertEquals("a***@yahoo.com", LogMaskingUtils.maskRecipient("a@yahoo.com"));
    }

    @Test
    public void testMaskRecipient_PhoneNumber() {
        assertEquals("09***12", LogMaskingUtils.maskRecipient("0912345612"));
        assertEquals("***", LogMaskingUtils.maskRecipient("123"));
    }

    @Test
    public void testMaskRecipient_NullOrEmpty() {
        assertEquals("null", LogMaskingUtils.maskRecipient(null));
        assertEquals("null", LogMaskingUtils.maskRecipient(""));
    }
}
