package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.entity.Favorite;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.FavoriteService;
import com.karpenko.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/shop/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;

    @GetMapping
    public String getFavorites(Model model) {
        User user = userService.getCurrentUser();
        List<Favorite> favorites = favoriteService.getFavoritesByUserId(user.getId());
        model.addAttribute("favorites", favorites);
        return "shop/favorites";
    }

    @PostMapping("/toggle/{productId}")
    public String toggleFavorite(@PathVariable Long productId) {
        User user = userService.getCurrentUser();
        favoriteService.toggleFavorite(user, productId);
        return "redirect:/shop/products";
    }
}