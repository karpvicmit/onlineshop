package com.karpenko.onlineshop.controller.auth;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.exception.EmailAlreadyExistsException;
import com.karpenko.onlineshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("registrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(
            @Valid @ModelAttribute("registrationDto") UserRegistrationDto dto,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            log.debug("Validierungsfehler bei Registrierung: {}", result.getAllErrors());
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
            log.info("Registrierung erfolgreich. Weiterleitung zur Login-Seite.");
            return "redirect:/login?registered";
        } catch (EmailAlreadyExistsException e) {
            log.warn("Registrierung abgelehnt: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            log.error("Unerwarteter Fehler bei Registrierung", e);
            model.addAttribute("errorMessage", "Ein technischer Fehler ist aufgetreten. Bitte versuchen Sie es später erneut.");
            return "auth/register";
        }
    }
}