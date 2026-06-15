package com.fleetmgm.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmgm.auth.application.AuthService;
import com.fleetmgm.auth.dto.AuthResponse;
import com.fleetmgm.auth.dto.LoginRequest;
import com.fleetmgm.auth.dto.RefreshRequest;
import com.fleetmgm.auth.infrastructure.JwtAuthenticationFilter;
import com.fleetmgm.shared.exception.BadCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    // SecurityConfig needs JwtAuthenticationFilter; mock it so the context loads without JwtService/UserRepository
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean AuthService authService;
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // --- POST /api/v1/auth/login ---

    @Test
    void login_returns200_whenValid() throws Exception {
        AuthResponse authResponse = new AuthResponse("access.token", "refresh.token", "DRIVER");
        when(authService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("driver@fleetmgm.com", "secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access.token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh.token"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void login_returns400_whenBodyEmpty() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_returns400_whenEmailMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secret\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void login_returns401_whenBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("driver@fleetmgm.com", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // --- POST /api/v1/auth/refresh ---

    @Test
    void refresh_returns200_whenValid() throws Exception {
        AuthResponse authResponse = new AuthResponse("new.access.token", "refresh.token", "DRIVER");
        when(authService.refresh(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("refresh.token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"));
    }

    @Test
    void refresh_returns400_whenTokenBlank() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void refresh_returns401_whenInvalid() throws Exception {
        when(authService.refresh(any())).thenThrow(new BadCredentialsException());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("bad.token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // --- POST /api/v1/auth/logout ---

    @Test
    void logout_returns204() throws Exception {
        doNothing().when(authService).logout(any());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest("refresh.token"))))
                .andExpect(status().isNoContent());
    }
}
