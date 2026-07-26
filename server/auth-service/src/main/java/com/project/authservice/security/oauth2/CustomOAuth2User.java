package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final Account account;

    public CustomOAuth2User(OAuth2User oauth2User, Account account) {
        this.oauth2User = oauth2User;
        this.account = account;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + account.getRole().getRoleName()));
    }

    @Override
    public String getName() {
        return account.getEmail();
    }
}
