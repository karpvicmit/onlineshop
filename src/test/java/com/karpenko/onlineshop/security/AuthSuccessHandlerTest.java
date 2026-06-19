package com.karpenko.onlineshop.security;

import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthSuccessHandler - Role-based Redirect")
class AuthSuccessHandlerTest {

    private AuthSuccessHandler authSuccessHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        authSuccessHandler = new AuthSuccessHandler();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("USER should be redirected to /shop/products after login")
    void shouldRedirectUserToProductList() throws IOException, ServletException {
        User user = createUser("user@test.de", Role.USER);
        Authentication auth = createAuthentication(user);

        authSuccessHandler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/shop/products");
    }

    @Test
    @DisplayName("ADMIN should be redirected to /admin/dashboard after login")
    void shouldRedirectAdminToDashboard() throws IOException, ServletException {
        User admin = createUser("admin@test.de", Role.ADMIN);
        Authentication auth = createAuthentication(admin);

        authSuccessHandler.onAuthenticationSuccess(request, response, auth);

        assertThat(response.getRedirectedUrl()).isEqualTo("/admin/dashboard");
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Authentication createAuthentication(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
    }
}