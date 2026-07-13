package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.dto.user.PasswordChangeDto;
import com.karpenko.onlineshop.dto.user.ProfileUpdateDto;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.ProfileService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;
    private final MessageUtil messageUtil;

    @GetMapping
    public String showProfile(Model model) {
        User currentUser = userService.getCurrentUser();
        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFirstName(currentUser.getFirstName());
        dto.setLastName(currentUser.getLastName());
        dto.setAddress(currentUser.getAddress());

        model.addAttribute("profileDto", dto);
        model.addAttribute("passwordDto", new PasswordChangeDto());
        model.addAttribute("userEmail", currentUser.getEmail());
        model.addAttribute("isOAuth2User",
                currentUser.getAuthProvider() != com.karpenko.onlineshop.entity.AuthProvider.LOCAL);
        return "/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("profileDto") @Valid ProfileUpdateDto dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "/profile";
        }

        User currentUser = userService.getCurrentUser();
        profileService.updateProfile(currentUser, dto);

        redirectAttributes.addFlashAttribute("successMessage",
                messageUtil.get("profile.update.success"));
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@ModelAttribute("passwordDto") @Valid PasswordChangeDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "/profile";
        }

        User currentUser = userService.getCurrentUser();
        try {
            userService.changePassword(currentUser, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    messageUtil.get("profile.password.success"));
            return "redirect:/profile";
        } catch (IllegalArgumentException ex) {
            log.warn("Password change failed: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile";
        } catch (IllegalStateException ex) {
            log.warn("Password change rejected: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/profile";
        }
    }
}