package com.karpenko.onlineshop.controller.auth;

import com.karpenko.onlineshop.config.BaseWebMvcTest;
import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.EmailAlreadyExistsException;
import com.karpenko.onlineshop.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController - Registration & Login")
class AuthControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @DisplayName("GET /login should return login page")
    void shouldShowLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /register should return registration form")
    void shouldShowRegistrationForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registrationDto"));
    }

    @Test
    @DisplayName("POST /register with valid data should redirect to login")
    void shouldRegisterUserSuccessfully() throws Exception {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("newuser@test.de");

        when(userService.registerUser(any(UserRegistrationDto.class)))
                .thenReturn(mockUser);

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "newuser@test.de")
                        .param("password", "SecurePass123")
                        .param("firstName", "Max")
                        .param("lastName", "Mustermann")
                        .param("address", "Berlin, Str. 1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    @Test
    @DisplayName("POST /register with duplicate email should show error")
    void shouldRejectDuplicateEmail() throws Exception {
        doThrow(new EmailAlreadyExistsException("E-Mail bereits registriert"))
                .when(userService).registerUser(any(UserRegistrationDto.class));

        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "existing@test.de")
                        .param("password", "SecurePass123")
                        .param("firstName", "Max")
                        .param("lastName", "Mustermann")
                        .param("address", "Berlin, Str. 1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attribute("errorMessage", "E-Mail bereits registriert"));
    }

    @Test
    @DisplayName("POST /register with invalid email should show validation error")
    void shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "invalid-email")
                        .param("password", "SecurePass123")
                        .param("firstName", "Max")
                        .param("lastName", "Mustermann")
                        .param("address", "Berlin, Str. 1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    @DisplayName("POST /register with short password should show validation error")
    void shouldRejectShortPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("email", "user@test.de")
                        .param("password", "short")
                        .param("firstName", "Max")
                        .param("lastName", "Mustermann")
                        .param("address", "Berlin, Str. 1"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));

        verify(userService, never()).registerUser(any());
    }
}