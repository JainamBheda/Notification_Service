package com.example.notification.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class ClientPrincipal implements UserDetails {

    private final String clientId;
    private final Collection<? extends GrantedAuthority> authorities;

    public ClientPrincipal(String clientId, List<String> roles) {
        this.clientId = clientId;
        List<String> effectiveRoles = roles == null || roles.isEmpty() ? List.of("CLIENT") : roles;
        this.authorities = effectiveRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return clientId;
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
