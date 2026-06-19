// src/test/java/com/karpenko/onlineshop/util/TestSecurityUtils.java
package com.karpenko.onlineshop.util;

import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.security.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility for setting CustomUserDetails in SecurityContext during tests.
 * Solves the problem of @WithMockUser incompatibility with custom UserDetails.
 */
public final class TestSecurityUtils {

    private TestSecurityUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sets the authentication with CustomUserDetails in the SecurityContext.
     *
     * @param userId ID пользователя
     * @param email  email пользователя
     * @param role   роль (USER или ADMIN)
     */
    public static void authenticateAs(Long userId, String email, Role role) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setAddress("Test Address 1, 12345 Berlin");

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
    }

    /**
     * Sets authentication to USER.
     */
    public static void authenticateAsUser(Long userId, String email) {
        authenticateAs(userId, email, Role.USER);
    }

    /**
     * Sets authentication to ADMIN.
     */
    public static void authenticateAsAdmin(Long userId, String email) {
        authenticateAs(userId, email, Role.ADMIN);
    }

    /**
     * Clean SecurityContext after test.
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}