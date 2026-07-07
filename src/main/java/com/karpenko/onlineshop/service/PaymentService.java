package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Order;

public interface PaymentService {
    /**
     * Создаёт PaymentIntent в Stripe и возвращает clientSecret
     */
    String createPaymentIntent(Order order);

    /**
     * Обрабатывает webhook событие от Stripe
     */
    void handleWebhookEvent(String payload, String signature);
}