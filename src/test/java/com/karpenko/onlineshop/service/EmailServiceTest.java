package com.karpenko.onlineshop.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

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
    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        Session session = Session.getDefaultInstance(new Properties());
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(session));
    }

    @Test
    @DisplayName("Should invoke JavaMailSender send method for confirmation email")
    void sendConfirmationEmail_shouldDelegateToMailSender() {
        emailService.sendConfirmationEmail("user@test.de", "token-123", "http://localhost:8080");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should wrap MailException in RuntimeException when SMTP fails")
    void sendConfirmationEmail_shouldThrowOnFailure() {
        doThrow(new MailSendException("SMTP server down"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() ->
                emailService.sendConfirmationEmail("fail@test.de", "token", "http://localhost:8080"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send confirmation email");
    }

    @Test
    @DisplayName("Should send password change notification via JavaMailSender")
    void sendPasswordChangeNotification_shouldDelegateToMailSender() {
        LocalDateTime changeDate = LocalDateTime.of(2026, 6, 27, 14, 30);

        emailService.sendPasswordChangeNotification("user@test.de", changeDate);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should NOT throw exception when SMTP fails during password change notification")
    void sendPasswordChangeNotification_shouldSwallowFailure() {
        doThrow(new MailSendException("SMTP server down"))
                .when(mailSender).send(any(MimeMessage.class));

        LocalDateTime changeDate = LocalDateTime.now();

        assertThatCode(() ->
                emailService.sendPasswordChangeNotification("user@test.de", changeDate))
                .as("Password change notification failure must not break the flow")
                .doesNotThrowAnyException();
    }
}