package com.project.authservice.security.oauth2;

import com.project.authservice.dto.response.JwtResponse;
import com.project.authservice.entity.Account;
import com.project.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuth2AuthenticationSuccessHandlerTest {

    @Test
    void redirectsTokensInFragmentInsteadOfQueryString() throws Exception {
        AuthService authService = mock(AuthService.class);
        Authentication authentication = mock(Authentication.class);
        CustomOAuth2User oauthUser = mock(CustomOAuth2User.class);
        Account account = new Account();
        when(authentication.getPrincipal()).thenReturn(oauthUser);
        when(oauthUser.getAccount()).thenReturn(account);
        when(authService.loginOAuth2(
                org.mockito.ArgumentMatchers.eq(account),
                org.mockito.ArgumentMatchers.any(MockHttpServletRequest.class)))
                .thenReturn(new JwtResponse(
                        "access.token",
                        "refresh+token",
                        900000L,
                        "user@example.com",
                        "CUSTOMER",
                        7L));

        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService);
        ReflectionTestUtils.setField(
                handler,
                "oauth2RedirectUrl",
                "http://localhost:5173/oauth2/redirect");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        String redirect = response.getRedirectedUrl();
        assertTrue(redirect.startsWith(
                "http://localhost:5173/oauth2/redirect#accessToken=access.token"));
        assertTrue(redirect.contains("&refreshToken=refresh%2Btoken"));
        assertFalse(redirect.contains("?accessToken="));
    }

    @Test
    void redirectsGoogleOidcLoginToFrontendCallback() throws Exception {
        AuthService authService = mock(AuthService.class);
        Authentication authentication = mock(Authentication.class);
        CustomOidcUser oidcUser = mock(CustomOidcUser.class);
        Account account = new Account();
        when(authentication.getPrincipal()).thenReturn(oidcUser);
        when(oidcUser.getAccount()).thenReturn(account);
        when(authService.loginOAuth2(
                org.mockito.ArgumentMatchers.eq(account),
                org.mockito.ArgumentMatchers.any(MockHttpServletRequest.class)))
                .thenReturn(new JwtResponse(
                        "google.access.token",
                        "google-refresh-token",
                        900000L,
                        "customer@example.com",
                        "CUSTOMER",
                        8L));

        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService);
        ReflectionTestUtils.setField(
                handler,
                "oauth2RedirectUrl",
                "http://localhost:5173/oauth2/redirect");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertTrue(response.getRedirectedUrl().startsWith(
                "http://localhost:5173/oauth2/redirect#accessToken=google.access.token"));
        verify(authService).loginOAuth2(account, request);
    }

    @Test
    void unsupportedPrincipalNeverFallsBackToApiGatewayRoot() throws Exception {
        AuthService authService = mock(AuthService.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(new Object());

        OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(authService);
        ReflectionTestUtils.setField(
                handler,
                "oauth2RedirectUrl",
                "http://localhost:5173/oauth2/redirect");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        assertTrue(response.getRedirectedUrl().startsWith(
                "http://localhost:5173/oauth2/redirect#error="));
        assertFalse("/".equals(response.getRedirectedUrl()));
    }
}
