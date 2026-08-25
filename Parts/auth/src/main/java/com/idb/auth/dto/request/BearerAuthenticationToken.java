package com.idb.auth.dto.request;

import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Authentication produced from a validated bearer token.
 *
 * <p>{@link #getCredentials()} carries a <em>newly minted</em> access token when
 * the presented one had expired and was renewed from the refresh token, and
 * {@code null} when the presented access token was still valid. {@code AuthFilter}
 * relies on that distinction to decide whether to echo an {@code x-access-token}
 * response header.
 */
public class BearerAuthenticationToken implements Authentication {

    @JsonProperty("username")
    private final String username;

    @JsonProperty("token")
    private final String token;

    @JsonProperty("isAuthenticated")
    private boolean authenticated;

    @JsonProperty("authorities")
    private Collection<? extends GrantedAuthority> authorities;

    public BearerAuthenticationToken(String username, String token, boolean isAuthenticated) {
        this.username = username;
        this.token = token;
        this.authenticated = isAuthenticated;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getDetails() {
        return username;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        this.authenticated = isAuthenticated;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        this.authorities = authorities;
    }
}
