package com.karpenko.onlineshop.controller.shop;

import com.karpenko.onlineshop.config.BaseWebMvcTest;
import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.PaymentProvider;
import com.karpenko.onlineshop.entity.ShippingMethod;
import com.karpenko.onlineshop.entity.User;
import com.karpenko.onlineshop.service.EmailService;
import com.karpenko.onlineshop.service.OrderService;
import com.karpenko.onlineshop.service.PaymentService;
import com.karpenko.onlineshop.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CheckoutController.class)
@DisplayName("CheckoutController - Payment & Shipping Selection")
class CheckoutControllerTest extends BaseWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private EmailService emailService;

    @Test
    @DisplayName("GET /shop/checkout should render checkout page")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldShowCheckoutPage() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setFirstName("Max");
        user.setLastName("Mustermann");
        user.setAddress("Test Street 1");

        when(userService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/shop/checkout"))
                .andExpect(status().isOk())
                .andExpect(view().name("shop/checkout"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    @DisplayName("POST /shop/checkout/confirm with VORKASSE should send order confirmation email")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldConfirmVorkasseOrderAndSendEmail() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setAddress("Test Street 1");

        Order order = new Order();
        order.setId(100L);
        order.setUser(user);

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderService.checkout(eq(user), any(), eq(PaymentProvider.VORKASSE), eq(ShippingMethod.STANDARD)))
                .thenReturn(order);

        mockMvc.perform(post("/shop/checkout/confirm")
                        .with(csrf())
                        .param("paymentProvider", "VORKASSE")
                        .param("shippingMethod", "STANDARD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/orders/100"));

        verify(emailService).sendOrderConfirmationEmail("user@test.de", order);
    }

    @Test
    @DisplayName("POST /shop/checkout/confirm with RECHNUNG should send invoice email")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldConfirmRechnungOrderAndSendInvoice() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setAddress("Test Street 1");

        Order order = new Order();
        order.setId(200L);
        order.setUser(user);

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderService.checkout(eq(user), any(), eq(PaymentProvider.RECHNUNG), eq(ShippingMethod.STANDARD)))
                .thenReturn(order);

        mockMvc.perform(post("/shop/checkout/confirm")
                        .with(csrf())
                        .param("paymentProvider", "RECHNUNG")
                        .param("shippingMethod", "STANDARD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/orders/200"));

        verify(emailService).sendInvoiceEmail("user@test.de", order);
        verify(emailService, never()).sendOrderConfirmationEmail(any(), any());
    }

    @Test
    @DisplayName("POST /shop/checkout/confirm with STRIPE should redirect to payment page")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldRedirectToStripePaymentPage() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setAddress("Test Street 1");

        Order order = new Order();
        order.setId(300L);
        order.setUser(user);

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderService.checkout(eq(user), any(), eq(PaymentProvider.STRIPE), eq(ShippingMethod.EXPRESS)))
                .thenReturn(order);
        when(paymentService.createPaymentIntent(order)).thenReturn("cs_test_123");

        mockMvc.perform(post("/shop/checkout/confirm")
                        .with(csrf())
                        .param("paymentProvider", "STRIPE")
                        .param("shippingMethod", "EXPRESS"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/payment?orderId=300"));

        // For Stripe, email is sent via webhook, not immediately
        verify(emailService, never()).sendOrderConfirmationEmail(any(), any());
        verify(emailService, never()).sendInvoiceEmail(any(), any());
    }

    @Test
    @DisplayName("POST /shop/checkout/confirm with EXPRESS shipping should apply correct cost")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldApplyExpressShipping() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setAddress("Test Street 1");

        Order order = new Order();
        order.setId(400L);
        order.setUser(user);

        when(userService.getCurrentUser()).thenReturn(user);
        when(orderService.checkout(eq(user), any(), eq(PaymentProvider.VORKASSE), eq(ShippingMethod.EXPRESS)))
                .thenReturn(order);

        mockMvc.perform(post("/shop/checkout/confirm")
                        .with(csrf())
                        .param("paymentProvider", "VORKASSE")
                        .param("shippingMethod", "EXPRESS"))
                .andExpect(status().is3xxRedirection());

        verify(orderService).checkout(eq(user), any(), eq(PaymentProvider.VORKASSE), eq(ShippingMethod.EXPRESS));
    }

    @Test
    @DisplayName("POST /shop/checkout/confirm with invalid payment should redirect back with error")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldRejectInvalidPaymentProvider() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");

        when(userService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(post("/shop/checkout/confirm")
                        .with(csrf())
                        .param("paymentProvider", "INVALID_PROVIDER")
                        .param("shippingMethod", "STANDARD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/checkout"));

        verify(orderService, never()).checkout(any(), any(), any(), any());
    }

    @Test
    @DisplayName("POST /shop/checkout/confirm with invalid shipping should redirect back with error")
    @WithMockUser(username = "user@test.de", roles = {"USER"})
    void shouldRejectInvalidShippingMethod() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");

        when(userService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(post("/shop/checkout/confirm")
                        .with(csrf())
                        .param("paymentProvider", "VORKASSE")
                        .param("shippingMethod", "INVALID_SHIPPING"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shop/checkout"));

        verify(orderService, never()).checkout(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Unauthenticated user should be redirected to /login")
    void shouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/shop/checkout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}