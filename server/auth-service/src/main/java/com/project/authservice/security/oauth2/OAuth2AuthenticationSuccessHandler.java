package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import com.project.authservice.service.AuthService;
import com.project.authservice.dto.response.JwtResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final AuthService authService;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof CustomOAuth2User) {
            CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
            Account account = oauthUser.getAccount();
            
            JwtResponse jwtResponse = authService.loginOAuth2(account, request);
            
            String targetUrl = frontendUrl + "/oauth2/redirect" + 
                    "?accessToken=" + jwtResponse.getAccessToken() + 
                    "&refreshToken=" + jwtResponse.getRefreshToken() + 
                    "&expiresIn=" + jwtResponse.getExpiresIn();
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
    public OAuth2AuthenticationSuccessHandler(AuthService authService) {
        this.authService = authService;
    }
}
