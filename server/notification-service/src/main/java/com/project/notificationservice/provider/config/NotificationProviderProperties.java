package com.project.notificationservice.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProviderProperties {

    private final Email email = new Email();
    private final MockEmail mockEmail = new MockEmail();

    public Email getEmail() {
        return email;
    }

    public MockEmail getMockEmail() {
        return mockEmail;
    }

    public static class Email {
        private String provider = "mock"; // mock or gmail

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    public static class MockEmail {
        private boolean simulateFailure = false;
        private String failureCode = "PROVIDER_TIMEOUT";
        private boolean retryable = true;

        public boolean isSimulateFailure() {
            return simulateFailure;
        }

        public void setSimulateFailure(boolean simulateFailure) {
            this.simulateFailure = simulateFailure;
        }

        public String getFailureCode() {
            return failureCode;
        }

        public void setFailureCode(String failureCode) {
            this.failureCode = failureCode;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public void setRetryable(boolean retryable) {
            this.retryable = retryable;
        }
    }
}
