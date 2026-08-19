package com.project.promotionservice.configuration.security.principal;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String email;
    private final List<String> roles;
    private final List<String> permissions;
    private final Set<String> cinemaPublicIds;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String email, List<String> roles, List<String> permissions, Collection<? extends GrantedAuthority> authorities) {
        this(id, username, email, roles, permissions, Set.of(), authorities);
    }

    public UserPrincipal(Long id, String username, String email, List<String> roles,
                         List<String> permissions, Set<String> cinemaPublicIds,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username != null ? username : (email != null ? email : (id != null ? id.toString() : "anonymous"));
        this.email = email;
        this.roles = roles != null ? roles : Collections.emptyList();
        this.permissions = permissions != null ? permissions : Collections.emptyList();
        this.cinemaPublicIds = cinemaPublicIds == null ? Set.of() : Set.copyOf(cinemaPublicIds);
        this.authorities = authorities != null ? authorities : Collections.emptyList();
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

    public Set<String> getCinemaPublicIds() {
        return cinemaPublicIds;
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
