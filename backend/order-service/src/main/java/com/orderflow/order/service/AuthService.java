package com.orderflow.order.service;

import com.orderflow.order.dto.auth.AuthResponse;
import com.orderflow.order.dto.auth.LoginRequest;
import com.orderflow.order.dto.auth.RegisterRequest;
import com.orderflow.order.entity.User;
import com.orderflow.order.repository.UserRepository;
import com.orderflow.order.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, 
                       JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Username already exists");
        }

        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole()
        );
        userRepository.save(user);

        log.info("Registered new user: {}", user.getUsername());
        return new AuthResponse(null, 0, user.getUsername(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            auditService.logAudit(request.getUsername(), "LOGIN", "USER", request.getUsername(), "FAILURE");
            log.warn("Authentication failed: username={}", request.getUsername());
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        auditService.logAudit(user.getUsername(), "LOGIN", "USER", user.getUsername(), "SUCCESS");
        log.info("Authentication successful: username={}", user.getUsername());

        return new AuthResponse(token, jwtService.getExpirationMs(), user.getUsername(), user.getRole());
    }
}
