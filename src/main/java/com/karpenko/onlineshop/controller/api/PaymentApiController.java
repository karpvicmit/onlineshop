package com.karpenko.onlineshop.controller.api;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.PaymentProvider;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.PaymentService;
import com.karpenko.onlineshop.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentApiController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<Map<String, String>> createPayment(@RequestBody Map<String, Long> request) {
        Long orderId = request.get("orderId");

        if (orderId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "orderId is required"));
        }

        User user = userService.getCurrentUser();
        Order order = orderService.getOrderDetails(orderId, user);

        if (!order.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        if (order.getPaymentProvider() == PaymentProvider.VORKASSE) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Vorkasse does not require online payment"));
        }

        try {
            String sessionId = paymentService.createPaymentIntent(order);
            return ResponseEntity.ok(Map.of("sessionId", sessionId));
        } catch (Exception e) {
            log.error("Failed to create payment for order {}", orderId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to create payment"));
        }
    }
}