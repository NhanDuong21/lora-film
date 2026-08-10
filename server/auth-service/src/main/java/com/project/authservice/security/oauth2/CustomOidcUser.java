package com.project.authservice.security.oauth2;

import com.project.authservice.entity.Account;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class CustomOidcUser implements OidcUser, AccountOAuth2Principal {
    private final OidcUser oidcUser;
    private final Account account;

    public CustomOidcUser(OidcUser oidcUser, Account account) {
        this.oidcUser = oidcUser;
        this.account = account;
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + account.getRole().getRoleName()));
    }

    @Override
    public String getName() {
        return account.getEmail();
    }

    @Override
    public Account getAccount() {
        return account;
    }
}
