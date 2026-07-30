package com.project.paymentservice.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Locale;

/**
 * Builds provider clients without weakening TLS verification.
 *
 * <p>Some supported Java 21 distributions predate Sectigo's newer public
 * E46 root, while the Windows root store already receives that trusted root
 * through the operating-system update channel. On Windows we therefore use
 * the OS trust store. Other platforms keep the JVM default trust store.</p>
 */
public final class ProviderHttpClientFactory {
    private static final Logger log =
            LoggerFactory.getLogger(ProviderHttpClientFactory.class);

    private ProviderHttpClientFactory() {
    }

    public static HttpClient create(Duration connectTimeout) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(connectTimeout);
        if (isWindows()) {
            try {
                builder.sslContext(windowsRootSslContext());
            } catch (Exception exception) {
                log.warn("Could not initialize Windows provider trust store; "
                                + "falling back to the JVM trust store: error={}",
                        exception.getClass().getSimpleName());
            }
        }
        return builder.build();
    }

    static SSLContext windowsRootSslContext() throws Exception {
        KeyStore windowsRoots = KeyStore.getInstance("Windows-ROOT");
        windowsRoots.load(null, null);
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(windowsRoots);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers.getTrustManagers(), null);
        return context;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }
}
