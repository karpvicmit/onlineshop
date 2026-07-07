package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.entity.Order;
import com.karpenko.onlineshop.entity.OrderStatus;
import com.karpenko.onlineshop.entity.PaymentProvider;
import com.karpenko.onlineshop.entity.PaymentStatus;
import com.karpenko.onlineshop.repository.OrderRepository;
import com.karpenko.onlineshop.service.EmailService;
import com.karpenko.onlineshop.service.PaymentService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripePaymentServiceImpl implements PaymentService {

    private final StripeClient stripeClient;
    private final OrderRepository orderRepository;
    private final EmailService emailService;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Override
    public String createPaymentIntent(Order order) {
        try {
            long amountInCents = order.getGrandTotal()
                    .multiply(new BigDecimal("100"))
                    .longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl("http://localhost:8080/shop/orders/" + order.getId()
                            + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl("http://localhost:8080/shop/checkout?cancelled=true")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Bestellung #" + order.getId())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("order_id", order.getId().toString())
                    .build();

            Session session = stripeClient.checkout().sessions().create(params);

            order.setPaymentProvider(PaymentProvider.STRIPE);
            order.setPaymentProviderId(session.getId());
            order.setPaymentStatus(PaymentStatus.PENDING);
            orderRepository.save(order);

            log.info("Stripe PaymentIntent created for order {}: {}", order.getId(), session.getId());
            return session.getId();

        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent for order {}", order.getId(), e);
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @Override
    @Transactional
    public void handleWebhookEvent(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(payload, signature, webhookSecret);
            log.info("Received Stripe webhook: {}", event.getType());

            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            StripeObject stripeObject = null;

            if (dataObjectDeserializer.getObject().isPresent()) {
                stripeObject = dataObjectDeserializer.getObject().get();
            } else {
                log.warn("Deserialization failed for Stripe event: {}", event.getId());
                return;
            }

            switch (event.getType()) {
                case "checkout.session.completed" -> handleCheckoutSessionCompleted(stripeObject);
                case "payment_intent.payment_failed" -> handlePaymentFailed(stripeObject);
                case "charge.refunded" -> handleChargeRefunded(stripeObject);
                default -> log.debug("Unhandled Stripe event type: {}", event.getType());
            }

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            throw new RuntimeException("Webhook processing failed", e);
        }
    }

    private void handleCheckoutSessionCompleted(StripeObject stripeObject) {
        Session session = (Session) stripeObject;
        String orderIdStr = session.getMetadata().get("order_id");

        if (orderIdStr == null) {
            log.warn("No order_id in Stripe session metadata");
            return;
        }

        Long orderId = Long.parseLong(orderIdStr);

        Optional<Order> orderOpt = orderRepository.findByIdWithUserAndItems(orderId);

        if (orderOpt.isEmpty()) {
            log.warn("Order {} not found for Stripe session {}", orderId, session.getId());
            return;
        }

        Order order = orderOpt.get();

        if (order.getPaymentStatus() == PaymentStatus.SUCCEEDED) {
            log.info("Order {} already marked as paid, ignoring duplicate webhook", orderId);
            return;
        }

        order.setPaymentStatus(PaymentStatus.SUCCEEDED);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("Order {} marked as paid via Stripe", orderId);

        try {
            emailService.sendPaymentSuccessEmail(order.getUser().getEmail(), order);
        } catch (Exception e) {
            log.error("Failed to send payment success email for order {}: {}",
                    orderId, e.getMessage());
        }
    }

    private void handlePaymentFailed(StripeObject stripeObject) {
        // TODO: Implement payment failed handling
        log.info("Payment failed event received");
    }

    private void handleChargeRefunded(StripeObject stripeObject) {
        // TODO: Implement refund handling
        log.info("Charge refunded event received");
    }
}