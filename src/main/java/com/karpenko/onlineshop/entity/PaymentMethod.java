package com.karpenko.onlineshop.entity;

/**
 * Payment methods available in the shop.
 * Display names and descriptions are localized via messages.properties
 * using keys: payment.{NAME}.name and payment.{NAME}.desc
 */
public enum PaymentMethod {
    VORKASSE,
    RECHNUNG,
    KREDITKARTE,
    PAYPAL,
    SOFORT
}