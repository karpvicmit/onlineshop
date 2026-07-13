package com.karpenko.onlineshop.controller.auth;

import com.karpenko.onlineshop.dto.user.UserRegistrationDto;
import com.karpenko.onlineshop.exception.EmailAlreadyExistsException;
import com.karpenko.onlineshop.service.UserService;
import com.karpenko.onlineshop.util.MessageUtil;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final MessageUtil messageUtil;

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
            log.info("Registrierung erfolgreich. E-Mail-Bestätigung gesendet.");
            return "redirect:/auth/email-sent";
        } catch (EmailAlreadyExistsException e) {
            log.warn("Registrierung abgelehnt: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            log.error("Unerwarteter Fehler bei Registrierung", e);
            model.addAttribute("errorMessage",
                    messageUtil.get("auth.register.error.technical"));
            return "auth/register";
        }
    }

    @GetMapping("/confirm-email")
    public String confirmEmail(@RequestParam String token, RedirectAttributes redirectAttributes) {
        try {
            userService.confirmEmail(token);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("auth.confirmEmail.success"));
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("auth.confirmEmail.invalid"));
            return "redirect:/login";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("auth.confirmEmail.expired"));
            return "redirect:/register";
        }
    }

    @GetMapping("/auth/email-sent")
    public String emailSent() {
        return "auth/email-sent";
    }
}