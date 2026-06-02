package com.ibb.onlineshop.controller;

import com.ibb.onlineshop.exception.EmailAlreadyExistsException;
import com.ibb.onlineshop.dto.user.UserRegistrationDto;
import com.ibb.onlineshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller für Authentifizierungs-Endpunkte (Login, Registrierung).
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "Ungültige E-Mail oder Passwort.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "Sie wurden erfolgreich abgemeldet.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("registrationDto") @Valid UserRegistrationDto dto,
                                      BindingResult bindingResult,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
            // Nach erfolgreicher Registrierung zur Login-Seite mit Erfolgsmeldung weiterleiten
            return "redirect:/login?registered=true";
        } catch (EmailAlreadyExistsException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        }
    }
}