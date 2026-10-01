package com.orderflow.order.security;

import com.orderflow.order.entity.User;
import com.orderflow.order.entity.UserRole;
import com.orderflow.order.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:}")
    private String adminUsername;

    @Value("${admin.password:}")
    private String adminPassword;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminUsername != null && !adminUsername.isBlank() && 
            adminPassword != null && !adminPassword.isBlank()) {
            
            if (!userRepository.existsByUsername(adminUsername)) {
                User admin = new User(adminUsername, passwordEncoder.encode(adminPassword), UserRole.ADMIN);
                userRepository.save(admin);
                log.info("Default admin user '{}' initialized for local development.", adminUsername);
            }
        }
    }
}
