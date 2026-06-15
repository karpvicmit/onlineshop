package com.karpenko.onlineshop.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    /**
     * Startseite: Weiterleitung basierend auf Authentifizierungsstatus.
     * - Eingeloggter USER → /shop/products
     * - Eingeloggter ADMIN → /admin/dashboard
     * - Nicht eingeloggter Benutzer → /login
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElse("");

            log.debug("Benutzer ist eingeloggt mit Rolle: {}", role);

            if (role.equals("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/shop/products";
            }
        }

        log.debug("Benutzer ist nicht eingeloggt, Weiterleitung zu /login");
        return "redirect:/shop/products";
    }

}