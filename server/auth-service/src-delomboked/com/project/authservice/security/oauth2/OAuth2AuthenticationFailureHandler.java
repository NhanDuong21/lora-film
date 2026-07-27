package com.project.authservice.security.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;

@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);
    @Value("${app.frontend.oauth2-redirect-url:http://localhost:5173/oauth2/redirect}")
    private String oauth2RedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.warn("OAuth2 authentication failed");
        String targetUrl = oauth2RedirectUrl + "#error=oauth2_authentication_failed";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
