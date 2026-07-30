package com.project.authservice.security.oauth2;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OAuth2AuthenticationFailureHandlerTest {

    @Test
    void redirectsErrorsToFrontendFragment() throws Exception {
        OAuth2AuthenticationFailureHandler handler =
                new OAuth2AuthenticationFailureHandler(
                        "http://localhost:5173/oauth2/redirect");
        AuthenticationException exception =
                new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_client"),
                        "The provided client secret is invalid.");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, exception);

        String redirect = response.getRedirectedUrl();
        assertEquals(
                "http://localhost:5173/oauth2/redirect"
                        + "#error=The+provided+client+secret+is+invalid.",
                redirect);
        assertFalse(redirect.contains("?error="));
    }
}
