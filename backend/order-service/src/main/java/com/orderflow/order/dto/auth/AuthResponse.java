package com.orderflow.order.dto.auth;

import com.orderflow.order.entity.UserRole;

public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String username;
    private UserRole role;

    public AuthResponse(String accessToken, long expiresIn, String username, UserRole role) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.username = username;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}
