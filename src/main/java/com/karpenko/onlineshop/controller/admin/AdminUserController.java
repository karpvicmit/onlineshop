package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "users"; }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("roles", Arrays.asList(Role.values()));
        model.addAttribute("statuses", Arrays.asList(UserStatus.values()));

        Long currentUserId = userService.getCurrentUserId();
        model.addAttribute("currentUserId", currentUserId);

        return "admin/users/list";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id, @RequestParam Role newRole, RedirectAttributes redirectAttributes) {
        log.info("Admin ändert die Rolle des Benutzers mit ID {} auf {}.", id, newRole);
        try {
            userService.updateUserRole(id, newRole);
            redirectAttributes.addFlashAttribute("successMessage", "Benutzerrolle erfolgreich aktualisiert.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam UserStatus newStatus, RedirectAttributes redirectAttributes) {
        log.info("Admin ändert den Status des Benutzers mit ID {} auf {}.", id, newStatus);
        try {
            userService.updateUserStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Benutzerstatus erfolgreich aktualisiert.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}