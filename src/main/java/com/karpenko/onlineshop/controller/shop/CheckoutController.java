package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.PaymentProvider;
import com.karpenko.onlineshop.entity.ShippingMethod;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.exception.ProductOutOfStockException;
import com.karpenko.onlineshop.service.EmailService;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.PaymentService;
import com.karpenko.onlineshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/shop/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final EmailService emailService; // ← restored

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @GetMapping
    public String showCheckoutPage(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "shop/checkout";
    }

    @PostMapping("/confirm")
    public String confirmOrder(
            @RequestParam("paymentProvider") String paymentProviderStr,
            @RequestParam(value = "shippingMethod", defaultValue = "STANDARD") String shippingMethodStr,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = userService.getCurrentUser();
        String promoCode = (String) session.getAttribute(CartController.SESSION_PROMO_CODE);

        PaymentProvider paymentProvider;
        try {
            paymentProvider = PaymentProvider.valueOf(paymentProviderStr);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ungültige Zahlungsart ausgewählt.");
            return "redirect:/shop/checkout";
        }

        ShippingMethod shippingMethod;
        try {
            shippingMethod = ShippingMethod.valueOf(shippingMethodStr);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ungültige Versandart ausgewählt.");
            return "redirect:/shop/checkout";
        }

        try {
            Order order = orderService.checkout(user, promoCode, paymentProvider, shippingMethod);
            session.removeAttribute(CartController.SESSION_PROMO_CODE);

            // 1. STRIPE → redirect to payment page, email sent later via webhook
            if (paymentProvider == PaymentProvider.STRIPE) {
                String sessionId = paymentService.createPaymentIntent(order);
                log.info("Stripe session created for order {}: {}", order.getId(), sessionId);
                return "redirect:/shop/payment?orderId=" + order.getId();
            }

            // 2. RECHNUNG → send invoice email immediately
            if (paymentProvider == PaymentProvider.RECHNUNG) {
                try {
                    emailService.sendInvoiceEmail(user.getEmail(), order);
                    log.info("Invoice email sent for order {}", order.getId());
                } catch (Exception e) {
                    log.warn("Failed to send invoice email for order {}: {}", order.getId(), e.getMessage());
                }
                String message = "Bestellung erfolgreich aufgegeben! "
                        + "Sie erhalten in Kürze eine E-Mail mit der Rechnung und unseren Bankdaten. "
                        + "Bestellnummer: #" + order.getId();
                redirectAttributes.addFlashAttribute("successMessage", message);
                return "redirect:/shop/orders/" + order.getId();
            }

            // 3. VORKASSE (and other offline) → send order confirmation email immediately
            try {
                emailService.sendOrderConfirmationEmail(user.getEmail(), order);
            } catch (Exception e) {
                log.warn("Failed to send order confirmation email for order {}: {}",
                        order.getId(), e.getMessage());
            }

            String message = "Bestellung erfolgreich aufgegeben! Bestellnummer: #" + order.getId();
            redirectAttributes.addFlashAttribute("successMessage", message);
            return "redirect:/shop/orders/" + order.getId();

        } catch (ProductOutOfStockException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/cart";
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shop/checkout";
        }
    }
}