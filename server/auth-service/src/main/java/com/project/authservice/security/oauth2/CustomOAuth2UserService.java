package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final OAuth2AccountService oAuth2AccountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2Profile profile = new OAuth2Profile(
                registrationId,
                firstAttribute(oauth2User, "sub", "id"),
                oauth2User.getAttribute("email"),
                oauth2User.getAttribute("name"),
                firstAttribute(oauth2User, "picture", "avatar_url"));
        Account account = oAuth2AccountService.findOrCreate(profile);
        return new CustomOAuth2User(oauth2User, account);
    }

    public CustomOAuth2UserService(OAuth2AccountService oAuth2AccountService) {
        this.oAuth2AccountService = oAuth2AccountService;
    }

    private String firstAttribute(OAuth2User user, String... names) {
        for (String name : names) {
            Object value = user.getAttribute(name);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
