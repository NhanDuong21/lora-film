package com.project.authservice.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 Authentication Failure: {}", exception.getMessage());
        String targetUrl = "/oauth2/redirect?error=" + exception.getMessage();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
