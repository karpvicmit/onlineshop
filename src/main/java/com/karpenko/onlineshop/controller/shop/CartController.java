package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.dto.CartDto;
import com.karpenko.onlineshop.exception.CartNotFoundException;
import com.karpenko.onlineshop.security.CustomUserDetails;
import com.karpenko.onlineshop.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/shop/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public String viewCart(Model model,
                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("User details: {}", userDetails);
        try {
            CartDto cartDto = cartService.getCartDtoForUser(userDetails.getId());
            model.addAttribute("cart", cartDto);
        } catch (CartNotFoundException e) {
            log.debug("Cart not found, showing empty cart.");
            model.addAttribute("cart", CartDto.builder().items(List.of()).total(BigDecimal.ZERO).itemCount(0).build());
        }
        return "shop/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @AuthenticationPrincipal CustomUserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            cartService.addItemToCart(userDetails.getId(), productId, quantity);
            log.info("User {} added product {} (qty: {}) to cart",
                    userDetails.getId(), productId, quantity);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            log.warn("Add to cart failed: {}", ex.getMessage());
        }
        return "redirect:/shop/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long productId,
                                 @RequestParam Integer quantity,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.updateItemQuantity(userDetails.getId(), productId, quantity);
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            log.warn("Quantity update failed: {}", ex.getMessage());
        }
        return "redirect:/shop/cart";
    }

    @PostMapping("/remove")
    public String removeItem(@RequestParam Long productId,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        cartService.removeItemFromCart(userDetails.getId(), productId);
        return "redirect:/shop/cart";
    }
}