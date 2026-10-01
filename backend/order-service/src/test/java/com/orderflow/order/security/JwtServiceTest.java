package com.orderflow.order.security;

import com.orderflow.order.entity.User;
import com.orderflow.order.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // 32-character secret
        String secret = "this-is-a-very-long-secret-key-that-is-at-least-256-bits-long";
        jwtService = new JwtService(secret, 3600000L); // 1 hour
    }

    @Test
    void generateAndValidateToken_Success() {
        User user = new User("testadmin", "hash", UserRole.ADMIN);
        
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        
        assertTrue(jwtService.validateToken(token));
        assertEquals("testadmin", jwtService.getUsernameFromToken(token));
        assertEquals("ADMIN", jwtService.getRoleFromToken(token));
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
    }
}
