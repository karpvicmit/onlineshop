package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.repository.CartRepository;
import com.karpenko.onlineshop.service.CartService;
import com.karpenko.onlineshop.service.PriceCalculatorService;
import com.karpenko.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@Controller
@RequestMapping("/shop/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;
    private final PriceCalculatorService priceCalculatorService;
    private final CartRepository cartRepository;

    @GetMapping
    public String viewCart(Model model) {
        User currentUser = userService.getCurrentUser();

        // Holen des Warenkorbs (oder eines leeren, wenn noch keiner existiert)
        Cart cart = cartRepository.findByUserId(currentUser.getId()).orElse(new Cart());

        BigDecimal total = priceCalculatorService.calculateTotal(cart);

        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", total);

        return "shop/cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        User currentUser = userService.getCurrentUser();
        cartService.addItemToCart(currentUser, productId, quantity);
        return "redirect:/shop/cart";
    }

    @PostMapping("/update")
    public String updateQuantity(@RequestParam Long productId, @RequestParam Integer quantity) {
        User currentUser = userService.getCurrentUser();
        cartService.updateItemQuantity(currentUser, productId, quantity);
        return "redirect:/shop/cart";
    }

    @PostMapping("/remove")
    public String removeItem(@RequestParam Long productId) {
        User currentUser = userService.getCurrentUser();
        cartService.removeItemFromCart(currentUser, productId);
        return "redirect:/shop/cart";
    }
}