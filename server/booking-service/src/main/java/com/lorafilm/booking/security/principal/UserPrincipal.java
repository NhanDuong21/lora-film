package com.lorafilm.booking.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;
    private final boolean identityVerified;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String email, List<String> roles,
            List<String> permissions, boolean identityVerified,
            Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username != null ? username : (email != null ? email : (id != null ? id.toString() : "anonymous"));
        this.email = email;
        this.roles = roles != null ? roles : Collections.emptyList();
        this.permissions = permissions != null ? permissions : Collections.emptyList();
        this.identityVerified = identityVerified;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
    }

    public UserPrincipal(Long id, String username, String email, List<String> roles,
            List<String> permissions, Collection<? extends GrantedAuthority> authorities) {
        this(id, username, email, roles, permissions, false, authorities);
    }

    public UserPrincipal(Long id, String email, Collection<? extends GrantedAuthority> authorities) {
        this(id, email, email, Collections.emptyList(), Collections.emptyList(), false, authorities);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public boolean isIdentityVerified() {
        return identityVerified;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
