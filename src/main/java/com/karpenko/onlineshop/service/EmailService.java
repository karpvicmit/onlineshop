package com.karpenko.onlineshop.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
        } catch (MessagingException | MailException e) {
            log.error("Failed to send confirmation email to: {}", to, e);
            throw new RuntimeException("Failed to send confirmation email", e);
        }
    }
    
    public void sendPasswordChangeNotification(String to, LocalDateTime changeDate) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Password Changed - OnlineShop Security Notification");

            String formattedDate = changeDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            String htmlContent = """
<html>
<body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4;">
<div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
<h2 style="color: #dc3545;"><i class="fas fa-exclamation-triangle"></i> Security Notification</h2>
<div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
<p style="margin: 0; color: #856404;"><strong>Password Change Detected</strong></p>
</div>
<p>Hello,</p>
<p>We're writing to inform you that your password was successfully changed on <strong>%s</strong>.</p>
<div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 15px; margin: 20px 0;">
<p style="margin: 0;"><strong>Details:</strong></p>
<ul style="margin: 10px 0; padding-left: 20px;">
<li>Account: <strong>%s</strong></li>
<li>Changed at: <strong>%s</strong></li>
<li>Action: Password updated</li>
</ul>
</div>
<div style="background-color: #d1ecf1; border-left: 4px solid #17a2b8; padding: 15px; margin: 20px 0;">
<p style="margin: 0; color: #0c5460;"><strong>Was this you?</strong></p>
<ul style="margin: 10px 0; padding-left: 20px;">
<li>If you made this change, you can safely ignore this email.</li>
<li>If you didn't change your password, please contact our support team immediately.</li>
</ul>
</div>
<p style="color: #666; font-size: 14px;">
For security reasons, we recommend:
<ul style="padding-left: 20px;">
<li>Using a strong, unique password</li>
<li>Never sharing your password with anyone</li>
<li>Changing your password regularly</li>
</ul>
</p>
<hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
<p style="color: #999; font-size: 12px;">
This is an automated security notification from OnlineShop.<br>
Please do not reply to this email.
</p>
</div>
</body>
</html>
""".formatted(formattedDate, to, formattedDate);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Password change notification sent to: {}", to);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send password change notification to: {}", to, e);
        }
    }
}