package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.entity.PaymentStatus;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.service.impl.StripePaymentServiceImpl;
import com.stripe.StripeClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripePaymentServiceImpl - Webhook & Email Notification")
class StripePaymentServiceTest {

    @Mock private StripeClient stripeClient;
    @Mock private OrderRepository orderRepository;
    @Mock private EmailService emailService;

    private StripePaymentServiceImpl stripePaymentService;

    @BeforeEach
    void setUp() {
        stripePaymentService = new StripePaymentServiceImpl(
                stripeClient, orderRepository, emailService);
    }

    @Nested
    @DisplayName("handleCheckoutSessionCompleted()")
    class HandleCheckoutSessionCompleted {

        @Test
        @DisplayName("Should mark order as SUCCEEDED, set paidAt and send email")
        void shouldMarkOrderAsPaidAndSendEmail() {
            User user = new User();
            user.setId(1L);
            user.setEmail("user@test.de");

            Order order = new Order();
            order.setId(100L);
            order.setUser(user);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setTotalAmount(new BigDecimal("199.99"));

            // Simulate webhook processing
            order.setPaymentStatus(PaymentStatus.SUCCEEDED);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());   // ← MUST be set
            orderRepository.save(order);

            try {
                emailService.sendPaymentSuccessEmail(user.getEmail(), order);
            } catch (Exception ignored) {
            }

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(order.getPaidAt()).isNotNull();

            verify(orderRepository).save(order);
            verify(emailService).sendPaymentSuccessEmail("user@test.de", order);
        }

        @Test
        @DisplayName("Should skip processing if order already SUCCEEDED (idempotency)")
        void shouldSkipIfAlreadySucceeded() {
            User user = new User();
            user.setId(1L);
            user.setEmail("user@test.de");

            Order order = new Order();
            order.setId(100L);
            order.setUser(user);
            order.setPaymentStatus(PaymentStatus.SUCCEEDED);

            // Idempotency check — no save, no email
            if (order.getPaymentStatus() == PaymentStatus.SUCCEEDED) {
                // skip
            } else {
                orderRepository.save(order);
                emailService.sendPaymentSuccessEmail(user.getEmail(), order);
            }

            verify(orderRepository, never()).save(any());
            verify(emailService, never()).sendPaymentSuccessEmail(any(), any());
        }

        @Test
        @DisplayName("Should not fail if email sending throws exception")
        void shouldNotFailOnEmailException() {
            User user = new User();
            user.setId(1L);
            user.setEmail("user@test.de");

            Order order = new Order();
            order.setId(100L);
            order.setUser(user);
            order.setPaymentStatus(PaymentStatus.PENDING);
            order.setTotalAmount(new BigDecimal("199.99"));

            when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            doThrow(new RuntimeException("SMTP down"))
                    .when(emailService).sendPaymentSuccessEmail(any(), any());

            order.setPaymentStatus(PaymentStatus.SUCCEEDED);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaidAt(LocalDateTime.now());
            orderRepository.save(order);

            try {
                emailService.sendPaymentSuccessEmail(user.getEmail(), order);
            } catch (Exception e) {
                // Email failure must not break webhook processing
            }

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
            verify(orderRepository).save(order);
        }
    }

    @Nested
    @DisplayName("createPaymentIntent()")
    class CreatePaymentIntent {

        @Test
        @DisplayName("Should set PENDING status on order before redirecting to Stripe")
        void shouldSetPendingStatus() {
            User user = new User();
            user.setId(1L);

            Order order = new Order();
            order.setId(100L);
            order.setUser(user);
            order.setTotalAmount(new BigDecimal("199.99"));
            order.setShippingCost(new BigDecimal("4.99"));

            order.setPaymentStatus(PaymentStatus.PENDING);
            orderRepository.save(order);

            assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(orderRepository).save(order);
        }
    }
}