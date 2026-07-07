package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.config.InvoiceProperties;
import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.PaymentMethod;
import com.karpenko.onlineshop.entity.ShippingMethod;
import com.karpenko.onlineshop.entity.User;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService - SMTP Interaction Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private InvoiceProperties invoiceProperties;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        Session session = Session.getDefaultInstance(new Properties());
        lenient().when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
    }

    // ========================================================================
    // sendConfirmationEmail()
    // ========================================================================
    @Nested
    @DisplayName("sendConfirmationEmail()")
    class SendConfirmationEmail {

        @Test
        @DisplayName("Should invoke JavaMailSender send method")
        void shouldDelegateToMailSender() {
            emailService.sendConfirmationEmail("user@test.de", "token-123", "http://localhost:8080");
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should wrap MailException in RuntimeException when SMTP fails")
        void shouldThrowOnFailure() {
            doThrow(new MailSendException("SMTP server down"))
                    .when(mailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() ->
                    emailService.sendConfirmationEmail("fail@test.de", "token", "http://localhost:8080"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send confirmation email");
        }
    }

    // ========================================================================
    // sendPasswordChangeNotification()
    // ========================================================================
    @Nested
    @DisplayName("sendPasswordChangeNotification()")
    class SendPasswordChangeNotification {

        @Test
        @DisplayName("Should send password change notification via JavaMailSender")
        void shouldDelegateToMailSender() {
            LocalDateTime changeDate = LocalDateTime.of(2026, 6, 27, 14, 30);
            emailService.sendPasswordChangeNotification("user@test.de", changeDate);
            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT throw exception when SMTP fails")
        void shouldSwallowFailure() {
            doThrow(new MailSendException("SMTP server down"))
                    .when(mailSender).send(any(MimeMessage.class));

            LocalDateTime changeDate = LocalDateTime.now();

            assertThatCode(() ->
                    emailService.sendPasswordChangeNotification("user@test.de", changeDate))
                    .as("Password change notification failure must not break the flow")
                    .doesNotThrowAnyException();
        }
    }

    // ========================================================================
    // sendOrderConfirmationEmail()
    // ========================================================================
    @Nested
    @DisplayName("sendOrderConfirmationEmail()")
    class SendOrderConfirmationEmail {

        @Test
        @DisplayName("Should send order confirmation email")
        void shouldSendEmail() {
            Order order = buildOrder(100L, new BigDecimal("199.99"));

            emailService.sendOrderConfirmationEmail("user@test.de", order);

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT throw when SMTP fails (order flow must not break)")
        void shouldSwallowSmtpFailure() {
            doThrow(new MailSendException("SMTP down"))
                    .when(mailSender).send(any(MimeMessage.class));

            Order order = buildOrder(100L, new BigDecimal("199.99"));

            assertThatCode(() -> emailService.sendOrderConfirmationEmail("user@test.de", order))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null orderDate gracefully")
        void shouldHandleNullOrderDate() {
            Order order = buildOrder(100L, new BigDecimal("199.99"));
            order.setOrderDate(null);

            assertThatCode(() -> emailService.sendOrderConfirmationEmail("user@test.de", order))
                    .doesNotThrowAnyException();
        }
    }

    // ========================================================================
    // sendPaymentSuccessEmail()
    // ========================================================================
    @Nested
    @DisplayName("sendPaymentSuccessEmail()")
    class SendPaymentSuccessEmail {

        @Test
        @DisplayName("Should send payment success email after Stripe webhook")
        void shouldSendEmail() {
            Order order = buildOrder(200L, new BigDecimal("499.00"));

            emailService.sendPaymentSuccessEmail("user@test.de", order);

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT throw when SMTP fails (webhook processing must not break)")
        void shouldSwallowSmtpFailure() {
            doThrow(new MailSendException("SMTP down"))
                    .when(mailSender).send(any(MimeMessage.class));

            Order order = buildOrder(200L, new BigDecimal("499.00"));

            assertThatCode(() -> emailService.sendPaymentSuccessEmail("user@test.de", order))
                    .doesNotThrowAnyException();
        }
    }

    // ========================================================================
    // sendInvoiceEmail() — Rechnung
    // ========================================================================
    @Nested
    @DisplayName("sendInvoiceEmail() — Rechnung")
    class SendInvoiceEmail {

        @BeforeEach
        void setUpInvoiceProperties() {
            lenient().when(invoiceProperties.getAccountHolder()).thenReturn("OnlineShop GmbH");
            lenient().when(invoiceProperties.getIban()).thenReturn("DE89370400440532013000");
            lenient().when(invoiceProperties.getBic()).thenReturn("COBADEFFXXX");
            lenient().when(invoiceProperties.getBankName()).thenReturn("Commerzbank Berlin");
            lenient().when(invoiceProperties.getPaymentTermDays()).thenReturn(14);
            lenient().when(invoiceProperties.getSupportEmail()).thenReturn("rechnung@onlineshop.de");
        }

        @Test
        @DisplayName("Should send invoice email with bank details")
        void shouldSendInvoiceEmail() {
            Order order = buildOrder(300L, new BigDecimal("1250.00"));

            emailService.sendInvoiceEmail("user@test.de", order);

            verify(mailSender, times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("Should NOT throw when SMTP fails")
        void shouldSwallowSmtpFailure() {
            doThrow(new MailSendException("SMTP down"))
                    .when(mailSender).send(any(MimeMessage.class));

            Order order = buildOrder(300L, new BigDecimal("1250.00"));

            assertThatCode(() -> emailService.sendInvoiceEmail("user@test.de", order))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null orderDate gracefully")
        void shouldHandleNullOrderDate() {
            Order order = buildOrder(300L, new BigDecimal("1250.00"));
            order.setOrderDate(null);

            assertThatCode(() -> emailService.sendInvoiceEmail("user@test.de", order))
                    .doesNotThrowAnyException();
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================
    private Order buildOrder(Long id, BigDecimal totalAmount) {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@test.de");
        user.setFirstName("Max");
        user.setLastName("Mustermann");

        Order order = new Order();
        order.setId(id);
        order.setUser(user);
        order.setOrderDate(LocalDateTime.of(2026, 7, 8, 10, 30));
        order.setTotalAmount(totalAmount);
        order.setShippingCost(new BigDecimal("4.99"));
        order.setShippingMethod(ShippingMethod.STANDARD);
        order.setPaymentMethod(PaymentMethod.VORKASSE);
        return order;
    }
}