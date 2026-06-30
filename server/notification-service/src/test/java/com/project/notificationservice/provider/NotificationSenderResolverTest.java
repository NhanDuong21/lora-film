package com.project.notificationservice.provider;

import com.project.notificationservice.enums.NotificationChannel;
import com.project.notificationservice.provider.config.NotificationProviderProperties;
import com.project.notificationservice.provider.email.GmailEmailSender;
import com.project.notificationservice.provider.email.MockEmailSender;
import com.project.notificationservice.provider.inapp.InAppNotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotificationSenderResolverTest {

    private MockEmailSender mockEmailSender;
    private GmailEmailSender gmailEmailSender;
    private InAppNotificationSender inAppNotificationSender;
    private NotificationProviderProperties properties;
    private NotificationSenderResolver resolver;

    @BeforeEach
    public void setUp() {
        mockEmailSender = mock(MockEmailSender.class);
        gmailEmailSender = mock(GmailEmailSender.class);
        inAppNotificationSender = mock(InAppNotificationSender.class);
        properties = new NotificationProviderProperties();
        resolver = new NotificationSenderResolver(mockEmailSender, gmailEmailSender, inAppNotificationSender, properties);
    }

    @Test
    public void testResolve_EmailMock() {
        properties.getEmail().setProvider("mock");
        NotificationSender sender = resolver.resolve(NotificationChannel.EMAIL);
        assertSame(mockEmailSender, sender);
    }

    @Test
    public void testResolve_EmailGmail() {
        properties.getEmail().setProvider("gmail");
        NotificationSender sender = resolver.resolve(NotificationChannel.EMAIL);
        assertSame(gmailEmailSender, sender);
    }

    @Test
    public void testResolve_EmailGmailCaseInsensitive() {
        properties.getEmail().setProvider("GMAIL");
        NotificationSender sender = resolver.resolve(NotificationChannel.EMAIL);
        assertSame(gmailEmailSender, sender);
    }

    @Test
    public void testResolve_InApp() {
        NotificationSender sender = resolver.resolve(NotificationChannel.IN_APP);
        assertSame(inAppNotificationSender, sender);
    }

    @Test
    public void testResolve_UnsupportedChannels() {
        assertNull(resolver.resolve(NotificationChannel.SMS));
        assertNull(resolver.resolve(NotificationChannel.PUSH_NOTIFICATION));
        assertNull(resolver.resolve(null));
    }
}
