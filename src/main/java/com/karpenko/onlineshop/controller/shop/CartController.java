// main/java/com/karpenko/onlineshop/controller/shop/CartController.java

package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.dto.cart.CartDto;
import com.karpenko.onlineshop.exception.CartNotFoundException;
import com.karpenko.onlineshop.security.CustomUserDetails;
import com.karpenko.onlineshop.service.CartService;
import jakarta.servlet.http.HttpSession;
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

    public static final String SESSION_PROMO_CODE = "PROMO_CODE";

    @GetMapping
    public String viewCart(Model model,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           HttpSession session) {
        log.debug("Viewing cart for user: {}", userDetails != null ? userDetails.getId() : "null");
        try {
            String promoCode = (String) session.getAttribute(SESSION_PROMO_CODE);
            CartDto cartDto = cartService.getCartDtoForUser(userDetails.getId(), promoCode);
            model.addAttribute("cart", cartDto);
        } catch (CartNotFoundException e) {
            log.debug("Cart not found, showing empty cart.");
            model.addAttribute("cart", CartDto.builder()
                    .items(List.of())
                    .total(BigDecimal.ZERO)
                    .itemCount(0)
                    .build());
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

    // === PROMO CODE ENDPOINTS ===

    @PostMapping("/promo/apply")
    public String applyPromoCode(@RequestParam String promoCode,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (promoCode == null || promoCode.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bitte geben Sie einen Promo-Code ein.");
            return "redirect:/shop/cart";
        }

        try {
            CartDto cartDto = cartService.getCartDtoForUser(userDetails.getId(), promoCode.trim());
            if (cartDto.getAppliedPromoCode() != null) {
                session.setAttribute(SESSION_PROMO_CODE, cartDto.getAppliedPromoCode());
                redirectAttributes.addFlashAttribute("successMessage",
                        "Promo-Code '" + cartDto.getAppliedPromoCode() + "' erfolgreich angewendet!");
                log.info("User {} applied promo code: {}", userDetails.getId(), cartDto.getAppliedPromoCode());
            } else if (cartDto.getPromoErrorMessage() != null) {
                session.removeAttribute(SESSION_PROMO_CODE);
                redirectAttributes.addFlashAttribute("errorMessage", cartDto.getPromoErrorMessage());
            }
        } catch (Exception ex) {
            log.warn("Failed to apply promo code: {}", ex.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler beim Anwenden des Promo-Codes.");
        }
        return "redirect:/shop/cart";
    }

    @PostMapping("/promo/remove")
    public String removePromoCode(HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        session.removeAttribute(SESSION_PROMO_CODE);
        redirectAttributes.addFlashAttribute("successMessage", "Promo-Code entfernt.");
        log.info("Promo code removed from session");
        return "redirect:/shop/cart";
    }
}