package com.karpenko.onlineshop.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendConfirmationEmail(String to, String token, String baseUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Registration confirmation - OnlineShop");

            String confirmationLink = baseUrl + "/confirm-email?token=" + token;

            String htmlContent = """
                <html>
                <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                        <h2 style="color: #333;">Welcome to OnlineShop!</h2>
                        <p>Thank you for registering. Please confirm your email address to complete the registration.</p>
                        <p style="margin: 30px 0;">
                            <a href="%s" 
                               style="background-color: #0d6efd; color: white; padding: 12px 30px; 
                                      text-decoration: none; border-radius: 5px; display: inline-block;">
                                Confirm email
                            </a>
                        </p>
                        <p style="color: #666; font-size: 14px;">
                            If the button doesn't work, copy and paste this link into your browser:<br>
                            <a href="%s" style="color: #0d6efd;">%s</a>
                        </p>
                        <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                        <p style="color: #999; font-size: 12px;">
                            This email was sent automatically. Please do not reply to it.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(confirmationLink, confirmationLink, confirmationLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Confirmation email sent to: {}", to);

            log.info("Confirmation email sent to: {}", to);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send confirmation email to: {}", to, e);
            throw new RuntimeException("Failed to send confirmation email", e);
        }
    }
}