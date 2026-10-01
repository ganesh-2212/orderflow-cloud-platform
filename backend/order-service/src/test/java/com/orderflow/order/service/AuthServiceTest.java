package com.orderflow.order.service;

import com.orderflow.order.dto.auth.AuthResponse;
import com.orderflow.order.dto.auth.LoginRequest;
import com.orderflow.order.dto.auth.RegisterRequest;
import com.orderflow.order.entity.User;
import com.orderflow.order.entity.UserRole;
import com.orderflow.order.repository.UserRepository;
import com.orderflow.order.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User("testuser", "hashedpass", UserRole.OPERATOR);
    }

    @Test
    void register_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("pass1234");
        req.setRole(UserRole.OPERATOR);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass1234")).thenReturn("hash1234");
        when(userRepository.save(any(User.class))).thenReturn(new User("newuser", "hash1234", UserRole.OPERATOR));

        AuthResponse res = authService.register(req);

        assertEquals("newuser", res.getUsername());
        assertEquals(UserRole.OPERATOR, res.getRole());
        assertNull(res.getAccessToken());
        verify(passwordEncoder, times(1)).encode("pass1234");
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("pass");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("pass", "hashedpass")).thenReturn(true);
        when(jwtService.generateToken(mockUser)).thenReturn("jwt.token.here");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        AuthResponse res = authService.login(req);

        assertEquals("jwt.token.here", res.getAccessToken());
        assertEquals("Bearer", res.getTokenType());
        assertEquals(3600000L, res.getExpiresIn());
        assertEquals("testuser", res.getUsername());
        
        verify(auditService, times(1)).logAudit("testuser", "LOGIN", "USER", "testuser", "SUCCESS");
    }

    @Test
    void login_InvalidPassword_ThrowsException() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrong");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrong", "hashedpass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
        verify(auditService, times(1)).logAudit("testuser", "LOGIN", "USER", "testuser", "FAILURE");
    }
}
