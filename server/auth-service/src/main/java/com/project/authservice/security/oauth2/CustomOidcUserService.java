package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {
    private final OAuth2AccountService oAuth2AccountService;

    public CustomOidcUserService(OAuth2AccountService oAuth2AccountService) {
        this.oAuth2AccountService = oAuth2AccountService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if ("google".equalsIgnoreCase(registrationId)
                && !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                    "Google account email has not been verified");
        }
        OAuth2Profile profile = new OAuth2Profile(
                registrationId,
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getFullName(),
                oidcUser.getPicture());
        Account account = oAuth2AccountService.findOrCreate(profile);
        return new CustomOidcUser(oidcUser, account);
    }
}
