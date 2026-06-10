package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.dto.user.ProfileUpdateDto;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.ProfileService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final ProfileService profileService;

    @GetMapping
    public String showProfile(Model model) {
        User currentUser = userService.getCurrentUser();

        ProfileUpdateDto dto = new ProfileUpdateDto();
        dto.setFirstName(currentUser.getFirstName());
        dto.setLastName(currentUser.getLastName());
        dto.setAddress(currentUser.getAddress());

        model.addAttribute("profileDto", dto);
        model.addAttribute("userEmail", currentUser.getEmail());

        return "shop/profile";
    }

    @PostMapping("/update")
    public String updateProfile(@ModelAttribute("profileDto") @Valid ProfileUpdateDto dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "shop/profile";
        }

        User currentUser = userService.getCurrentUser();
        profileService.updateProfile(currentUser, dto);

        redirectAttributes.addFlashAttribute("successMessage", "Profil erfolgreich aktualisiert.");
        return "redirect:/profile";
    }
}