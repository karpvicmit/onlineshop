package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.config.InvoiceProperties;
import com.karpenko.onlineshop.entity.Order;
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
    private final InvoiceProperties invoiceProperties;

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

    /**
     * Sends an order confirmation email after a successful offline order (e.g. Vorkasse).
     * Failure is logged but does not break the order flow.
     */
    public void sendOrderConfirmationEmail(String to, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Bestellbestätigung #" + order.getId() + " - OnlineShop");

            String orderDate = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            String shippingName = order.getShippingMethod() != null
                    ? order.getShippingMethod().getDisplayName() : "Standard";
            String paymentName = order.getPaymentMethod() != null
                    ? order.getPaymentMethod().getDisplayName() : "Vorkasse";

            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                            <h2 style="color: #198754;">✓ Vielen Dank für Ihre Bestellung!</h2>
                            <p>Wir haben Ihre Bestellung erhalten und werden sie schnellstmöglich bearbeiten.</p>
                            <div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 20px; margin: 20px 0;">
                                <p style="margin: 0 0 8px 0;"><strong>Bestellnummer:</strong> #%d</p>
                                <p style="margin: 0 0 8px 0;"><strong>Bestelldatum:</strong> %s</p>
                                <p style="margin: 0 0 8px 0;"><strong>Gesamtbetrag:</strong> %.2f €</p>
                                <p style="margin: 0 0 8px 0;"><strong>Versandart:</strong> %s</p>
                                <p style="margin: 0;"><strong>Zahlungsart:</strong> %s</p>
                            </div>
                            <p>Sie können den Status Ihrer Bestellung jederzeit in Ihrem Kundenkonto einsehen.</p>
                            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                            <p style="color: #999; font-size: 12px;">
                                Dies ist eine automatische Benachrichtigung. Bitte antworten Sie nicht auf diese E-Mail.
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(order.getId(), orderDate, order.getGrandTotal(),
                    shippingName, paymentName);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Order confirmation email sent to: {} for order #{}", to, order.getId());

        } catch (MessagingException | MailException e) {
            log.error("Failed to send order confirmation email to: {} for order #{}",
                    to, order.getId(), e);
        }
    }

    /**
     * Sends a payment success notification after Stripe webhook confirms the payment.
     * Failure is logged but does not break the webhook processing.
     */
    public void sendPaymentSuccessEmail(String to, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Zahlung bestätigt - Bestellung #" + order.getId());

            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                            <h2 style="color: #198754;">✓ Zahlung erfolgreich!</h2>
                            <p>Ihre Zahlung für die Bestellung <strong>#%d</strong> wurde erfolgreich verarbeitet.</p>
                            <div style="background-color: #d1ecf1; border-left: 4px solid #17a2b8; padding: 15px; margin: 20px 0;">
                                <p style="margin: 0 0 8px 0; color: #0c5460;"><strong>Bestelldetails:</strong></p>
                                <ul style="margin: 0; padding-left: 20px; color: #0c5460;">
                                    <li>Bestellnummer: <strong>#%d</strong></li>
                                    <li>Betrag: <strong>%.2f €</strong></li>
                                    <li>Status: <strong>Bestätigt</strong></li>
                                </ul>
                            </div>
                            <p>Ihre Bestellung wird nun bearbeitet und in Kürze versendet.
                               Sie können den Status jederzeit in Ihrem Kundenkonto verfolgen.</p>
                            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                            <p style="color: #999; font-size: 12px;">
                                Dies ist eine automatische Benachrichtigung. Bitte antworten Sie nicht auf diese E-Mail.
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(order.getId(), order.getId(), order.getGrandTotal());

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Payment success email sent to: {} for order #{}", to, order.getId());

        } catch (MessagingException | MailException e) {
            log.error("Failed to send payment success email to: {} for order #{}",
                    to, order.getId(), e);
        }
    }

    /**
     * Sends an invoice email for "Rechnung" (pay-by-invoice) payment method.
     * Contains bank details (IBAN, BIC), order amount, payment reference and due date.
     */
    public void sendInvoiceEmail(String to, Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Rechnung #" + order.getId() + " - OnlineShop");

            String orderDate = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            LocalDateTime dueDate = (order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now())
                    .plusDays(invoiceProperties.getPaymentTermDays());
            String dueDateStr = dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String paymentReference = "Rechnung Nr. " + order.getId();

            String htmlContent = """
                    <html>
                    <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4;">
                        <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 10px;">
                            <h2 style="color: #0d6efd;">Rechnung #%d</h2>
                            <p>Sehr geehrte(r) Kunde(in),</p>
                            <p>vielen Dank für Ihre Bestellung. Anbei erhalten Sie die Rechnung mit allen Zahlungsinformationen.</p>

                            <div style="background-color: #f8f9fa; border: 1px solid #dee2e6; border-radius: 5px; padding: 20px; margin: 20px 0;">
                                <h3 style="margin-top: 0; color: #333;">Rechnungsdetails</h3>
                                <p style="margin: 0 0 8px 0;"><strong>Rechnungsnummer:</strong> #%d</p>
                                <p style="margin: 0 0 8px 0;"><strong>Rechnungsdatum:</strong> %s</p>
                                <p style="margin: 0 0 8px 0;"><strong>Zahlbar bis:</strong> <span style="color: #dc3545;"><strong>%s</strong></span></p>
                                <p style="margin: 0;"><strong>Rechnungsbetrag:</strong> <span style="font-size: 1.3em; color: #0d6efd;"><strong>%.2f €</strong></span></p>
                            </div>

                            <div style="background-color: #fff3cd; border-left: 4px solid #ffc107; padding: 15px; margin: 20px 0;">
                                <p style="margin: 0 0 8px 0; color: #856404;"><strong>⚠ Wichtig: Verwendungszweck</strong></p>
                                <p style="margin: 0; font-size: 1.1em; color: #856404;">
                                    Bitte geben Sie als <strong>Verwendungszweck</strong> unbedingt an:<br>
                                    <code style="background: white; padding: 4px 8px; border-radius: 3px; font-size: 1.1em;">%s</code>
                                </p>
                            </div>

                            <div style="background-color: #e7f3ff; border: 1px solid #b6d4fe; border-radius: 5px; padding: 20px; margin: 20px 0;">
                                <h3 style="margin-top: 0; color: #0d6efd;">Bankverbindung</h3>
                                <p style="margin: 0 0 8px 0;"><strong>Kontoinhaber:</strong> %s</p>
                                <p style="margin: 0 0 8px 0;"><strong>IBAN:</strong> <code>%s</code></p>
                                <p style="margin: 0 0 8px 0;"><strong>BIC:</strong> <code>%s</code></p>
                                <p style="margin: 0;"><strong>Bank:</strong> %s</p>
                            </div>

                            <p>Bitte überweisen Sie den Rechnungsbetrag bis zum <strong>%s</strong> auf das oben genannte Konto.</p>
                            <p>Bei Fragen zu Ihrer Rechnung kontaktieren Sie uns bitte unter 
                               <a href="mailto:%s">%s</a>.</p>

                            <p>Mit freundlichen Grüßen,<br>Ihr OnlineShop-Team</p>

                            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                            <p style="color: #999; font-size: 12px;">
                                Dies ist eine automatische Benachrichtigung. Bitte antworten Sie nicht direkt auf diese E-Mail.
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(
                    order.getId(),
                    order.getId(), orderDate, dueDateStr, order.getGrandTotal(),
                    paymentReference,
                    invoiceProperties.getAccountHolder(),
                    invoiceProperties.getIban(),
                    invoiceProperties.getBic(),
                    invoiceProperties.getBankName(),
                    dueDateStr,
                    invoiceProperties.getSupportEmail(),
                    invoiceProperties.getSupportEmail()
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Invoice email sent to: {} for order #{}", to, order.getId());

        } catch (MessagingException | MailException e) {
            log.error("Failed to send invoice email to: {} for order #{}",
                    to, order.getId(), e);
        }
    }
}