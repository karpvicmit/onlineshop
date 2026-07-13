package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.entity.Role;
import com.karpenko.onlineshop.entity.UserStatus;
import com.karpenko.onlineshop.service.UserService;
import com.karpenko.onlineshop.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;

@Slf4j
@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;
    private final MessageUtil messageUtil;

    @ModelAttribute("activeMenu")
    public String activeMenu() { return "users"; }

    @GetMapping
    public String listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<User> userPage = userService.getAllUsers(pageable);

        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("baseUrl", "/admin/users");
        model.addAttribute("roles", Arrays.asList(Role.values()));
        model.addAttribute("statuses", Arrays.asList(UserStatus.values()));

        Long currentUserId = userService.getCurrentUserId();
        model.addAttribute("currentUserId", currentUserId);
        return "admin/users/list";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam Role newRole,
                             RedirectAttributes redirectAttributes) {
        log.info("Admin ändert die Rolle des Benutzers mit ID {} auf {}.", id, newRole);
        try {
            userService.updateUserRole(id, newRole);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.users.role.success"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.users.role.error", e.getMessage()));
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam UserStatus newStatus,
                               RedirectAttributes redirectAttributes) {
        log.info("Admin ändert den Status des Benutzers mit ID {} auf {}.", id, newStatus);
        try {
            userService.updateUserStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("admin.users.status.success"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    messageUtil.get("admin.users.status.error", e.getMessage()));
        }
        return "redirect:/admin/users";
    }
}