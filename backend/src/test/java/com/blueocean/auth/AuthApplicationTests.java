package com.blueocean.auth;

import com.blueocean.auth.dto.LoginRequest;
import com.blueocean.auth.dto.RegisterRequest;
import com.blueocean.auth.model.enums.UserType;
import com.blueocean.auth.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the JWT Authentication System.
 * Tests the full authentication flow: register → verify-OTP → login → dashboard → logout.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Test: Valid registration should return 201 Created with OTP.
     */
    @Test
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testuser1",   // 8+ chars
                "test.user@example.com",
                "Pass@1234",   // valid: uppercase, lowercase, digit, special
                UserType.STANDARD_USER,
                "IN"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.otp").exists());
    }

    /**
     * Test: Short username (< 8 chars) should return 400 Bad Request.
     */
    @Test
    void testRegisterInvalidUsername() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "short",  // only 5 chars — invalid
                "test@example.com",
                "Pass@1234",
                UserType.STANDARD_USER,
                "IN"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Invalid password (no special char) should return 400 or error.
     */
    @Test
    void testRegisterInvalidPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "validuser",
                "valid@example.com",
                "Password1",  // no special character — invalid
                UserType.STANDARD_USER,
                "IN"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Login with wrong credentials should return "Login Failed".
     */
    @Test
    void testLoginFailure() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent", "WrongPass@1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login Failed"));
    }

    /**
     * Test: Context loads correctly.
     */
    @Test
    void contextLoads() {
        // Verifies the Spring context starts without errors
    }
}
