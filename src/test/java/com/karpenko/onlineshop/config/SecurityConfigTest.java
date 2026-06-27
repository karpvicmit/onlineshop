package com.karpenko.onlineshop.config;

import com.karpenko.onlineshop.entity.AuthProvider;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.repository.UserRepository;
import com.karpenko.onlineshop.util.TestSecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Security Configuration - Access Control")
class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        // Create test user in DB
        testUser = new User();
        testUser.setEmail("user@test.de");
        testUser.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        testUser.setRole(Role.USER);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setAddress("Test Address 1, 12345 Berlin");
        testUser.setEmailConfirmed(true);
        testUser.setAuthProvider(AuthProvider.LOCAL);
        testUser = userRepository.save(testUser);

        // Create test admin in DB
        testAdmin = new User();
        testAdmin.setEmail("admin@test.de");
        testAdmin.setPasswordHash(passwordEncoder.encode("SecurePass123"));
        testAdmin.setRole(Role.ADMIN);
        testAdmin.setStatus(UserStatus.ACTIVE);
        testAdmin.setFirstName("Admin");
        testAdmin.setLastName("User");
        testAdmin.setAddress("Admin Address 1, 12345 Berlin");
        testAdmin.setEmailConfirmed(true);
        testAdmin.setAuthProvider(AuthProvider.LOCAL);
        testAdmin = userRepository.save(testAdmin);
    }

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
        // Use the actual user ID from DB
        TestSecurityUtils.authenticateAsUser(testUser.getId(), "user@test.de");
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Authenticated ADMIN accessing /admin/** should succeed")
    void shouldAllowAdminAccessToAdminArea() throws Exception {
        // Use the actual admin ID from DB
        TestSecurityUtils.authenticateAsAdmin(testAdmin.getId(), "admin@test.de");
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
        TestSecurityUtils.authenticateAsUser(testUser.getId(), "user@test.de");
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
        TestSecurityUtils.authenticateAsUser(testUser.getId(), "user@test.de");
        mockMvc.perform(get("/shop/favorites"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated USER should access /shop/orders")
    void shouldAllowUserAccessToOrders() throws Exception {
        TestSecurityUtils.authenticateAsUser(testUser.getId(), "user@test.de");
        mockMvc.perform(get("/shop/orders"))
                .andExpect(status().isOk());
    }
}