package com.project.authservice.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);
    private final String oauth2RedirectUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.frontend.oauth2-redirect-url}") String oauth2RedirectUrl) {
        this.oauth2RedirectUrl = oauth2RedirectUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 Authentication Failure: {}", exception.getMessage());
        String errorMessage = exception.getMessage() != null ? exception.getMessage() : "Authentication failed";
        String encodedError = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
        String targetUrl = oauth2RedirectUrl + "#error=" + encodedError;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
