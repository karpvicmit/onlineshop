package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.config.InvoiceProperties;
import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.util.MessageUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final InvoiceProperties invoiceProperties;
    private final TemplateEngine templateEngine;
    private final MessageUtil messageUtil;

    /**
     * Sends registration confirmation email with a confirmation link.
     */
    public void sendConfirmationEmail(String to, String token, String baseUrl) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String confirmationLink = baseUrl + "/confirm-email?token=" + token;

            Context context = new Context(locale);
            context.setVariable("confirmationLink", confirmationLink);

            String htmlContent = templateEngine.process("mail/email-confirmation", context);
            String subject = messageUtil.getForLocale("email.confirmation.subject", locale);

            sendHtmlEmail(to, subject, htmlContent);
            log.info("Confirmation email sent to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send confirmation email to: {}", to, e);
            throw new RuntimeException("Failed to send confirmation email", e);
        }
    }

    /**
     * Sends password change notification email.
     * Failure is logged but does not break the password change flow.
     */
    public void sendPasswordChangeNotification(String to, LocalDateTime changeDate) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String formattedDate = changeDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            Context context = new Context(locale);
            context.setVariable("userEmail", to);
            context.setVariable("formattedDate", formattedDate);

            String htmlContent = templateEngine.process("mail/password-change", context);
            String subject = messageUtil.getForLocale("email.passwordChange.subject", locale);

            sendHtmlEmail(to, subject, htmlContent);
            log.info("Password change notification sent to: {}", to);
        } catch (MailException e) {
            log.error("Failed to send password change notification to: {}", to, e);
        }
    }

    /**
     * Sends an order confirmation email after a successful offline order (e.g. Vorkasse).
     * Failure is logged but does not break the order flow.
     */
    public void sendOrderConfirmationEmail(String to, Order order) {
        try {
            Locale locale = LocaleContextHolder.getLocale();
            String orderDate = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

            String shippingName = messageUtil.getForLocale(
                    "shipping." + order.getShippingMethod().name() + ".name", locale);
            String paymentName = messageUtil.getForLocale(
                    "payment." + order.getPaymentMethod().name() + ".name", locale);

            Context context = new Context(locale);
            context.setVariable("orderId", order.getId());
            context.setVariable("orderDate", orderDate);
            context.setVariable("grandTotal", order.getGrandTotal().setScale(2, java.math.RoundingMode.HALF_UP));
            context.setVariable("shippingName", shippingName);
            context.setVariable("paymentName", paymentName);

            String htmlContent = templateEngine.process("mail/order-confirmation", context);
            String subject = messageUtil.getForLocale("email.orderConfirmation.subject", locale, order.getId());

            sendHtmlEmail(to, subject, htmlContent);
            log.info("Order confirmation email sent to: {} for order #{}", to, order.getId());
        } catch (MailException e) {
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
            Locale locale = LocaleContextHolder.getLocale();

            Context context = new Context(locale);
            context.setVariable("orderId", order.getId());
            context.setVariable("grandTotal", order.getGrandTotal().setScale(2, java.math.RoundingMode.HALF_UP));

            String htmlContent = templateEngine.process("mail/payment-success", context);
            String subject = messageUtil.getForLocale("email.paymentSuccess.subject", locale, order.getId());

            sendHtmlEmail(to, subject, htmlContent);
            log.info("Payment success email sent to: {} for order #{}", to, order.getId());
        } catch (MailException e) {
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
            Locale locale = LocaleContextHolder.getLocale();
            String orderDate = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            LocalDateTime dueDate = (order.getOrderDate() != null ? order.getOrderDate() : LocalDateTime.now())
                    .plusDays(invoiceProperties.getPaymentTermDays());
            String dueDateStr = dueDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String paymentReference = messageUtil.getForLocale("email.invoice.referenceText", locale)
                    + " " + order.getId();

            Context context = new Context(locale);
            context.setVariable("orderId", order.getId());
            context.setVariable("orderDate", orderDate);
            context.setVariable("dueDate", dueDateStr);
            context.setVariable("grandTotal", order.getGrandTotal().setScale(2, java.math.RoundingMode.HALF_UP));
            context.setVariable("paymentReference", paymentReference);
            context.setVariable("accountHolder", invoiceProperties.getAccountHolder());
            context.setVariable("iban", invoiceProperties.getIban());
            context.setVariable("bic", invoiceProperties.getBic());
            context.setVariable("bankName", invoiceProperties.getBankName());
            context.setVariable("supportEmail", invoiceProperties.getSupportEmail());

            String htmlContent = templateEngine.process("mail/invoice", context);
            String subject = messageUtil.getForLocale("email.invoice.subject", locale, order.getId());

            sendHtmlEmail(to, subject, htmlContent);
            log.info("Invoice email sent to: {} for order #{}", to, order.getId());
        } catch (MailException e) {
            log.error("Failed to send invoice email to: {} for order #{}",
                    to, order.getId(), e);
        }
    }

    /**
     * Helper method to send HTML email.
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to create MIME message for email to: {}", to, e);
            throw new MailException("Failed to create MIME message", e) {};
        }
    }
}