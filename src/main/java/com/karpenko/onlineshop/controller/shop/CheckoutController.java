package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/shop/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    public String showCheckoutPage(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        return "shop/checkout";
    }

    @PostMapping("/confirm")
    public String confirmOrder(HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser();
        String promoCode = (String) session.getAttribute(CartController.SESSION_PROMO_CODE);

        try {
            Order order = orderService.checkout(user, promoCode);

            session.removeAttribute(CartController.SESSION_PROMO_CODE);

            String message = "Bestellung erfolgreich aufgegeben! Bestellnummer: " + order.getId();
            if (order.getDiscountAmount() != null
                    && order.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                message += " (Rabatt: " + order.getDiscountAmount() + " €)";
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
            return "redirect:/shop/orders";
        } catch (ProductOutOfStockException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/cart";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/cart";
        }
    }
}