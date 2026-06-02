package com.ibb.onlineshop.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Leitet den Benutzer nach erfolgreichem Login basierend auf seiner Rolle weiter.
 * USER -> /shop/products
 * ADMIN -> /admin/dashboard
 */
@Slf4j
@Component
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        String redirectUrl = "/shop/products"; // Standard-Weiterleitung für USER

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (authority.getAuthority().equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            }
        }

        log.info("Erfolgreicher Login. Weiterleitung zu: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}