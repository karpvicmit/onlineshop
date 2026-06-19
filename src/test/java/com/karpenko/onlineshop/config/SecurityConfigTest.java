package com.karpenko.onlineshop.config;

import com.karpenko.onlineshop.util.TestSecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Security Configuration - Access Control")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void tearDown() {
        TestSecurityUtils.clearAuthentication();
    }

    @Test
    @DisplayName("Unauthenticated user accessing /admin/** should redirect to /login")
    void shouldRedirectToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Authenticated USER accessing /admin/** should get 403")
    void shouldReturn403WhenUserAccessesAdminArea() throws Exception {
        TestSecurityUtils.authenticateAsUser(1L, "user@test.de");

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated ADMIN accessing /admin/** should succeed")
    void shouldAllowAdminAccessToAdminArea() throws Exception {
        TestSecurityUtils.authenticateAsAdmin(1L, "admin@test.de");

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Public endpoints should be accessible without authentication")
    void shouldAllowPublicAccess() throws Exception {
        mockMvc.perform(get("/shop/products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated USER should access /shop/cart")
    void shouldAllowUserAccessToCart() throws Exception {
        TestSecurityUtils.authenticateAsUser(1L, "user@test.de");

        mockMvc.perform(get("/shop/cart"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthenticated user accessing /shop/cart should redirect to /login")
    void shouldRedirectToLoginForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/shop/cart"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("Authenticated USER should access /shop/favorites")
    void shouldAllowUserAccessToFavorites() throws Exception {
        TestSecurityUtils.authenticateAsUser(1L, "user@test.de");

        mockMvc.perform(get("/shop/favorites"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated USER should access /shop/orders")
    void shouldAllowUserAccessToOrders() throws Exception {
        TestSecurityUtils.authenticateAsUser(1L, "user@test.de");

        mockMvc.perform(get("/shop/orders"))
                .andExpect(status().isOk());
    }
}