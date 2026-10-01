package com.orderflow.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.order.dto.auth.LoginRequest;
import com.orderflow.order.dto.auth.RegisterRequest;
import com.orderflow.order.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Assumes H2 in test
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void unauthenticatedAccessToProtectedEndpoint_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void invalidToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void registerAndLoginFlow() throws Exception {
        // Register
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("integrationuser");
        reg.setPassword("securePassword1!");
        reg.setRole(UserRole.OPERATOR);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("integrationuser"))
                .andExpect(jsonPath("$.accessToken").doesNotExist()); // No token returned on register

        // Login
        LoginRequest login = new LoginRequest();
        login.setUsername("integrationuser");
        login.setPassword("securePassword1!");

        String token = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        // Parse token
        String jwt = objectMapper.readTree(token).get("accessToken").asText();

        // Access Protected Endpoint (Operator can view orders)
        mockMvc.perform(get("/api/v1/orders")
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk()); // Returns 200 OK because OPERATOR has access

        // Access Admin Endpoint (Operator cannot access audit logs)
        mockMvc.perform(get("/api/v1/audit-logs")
                .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
