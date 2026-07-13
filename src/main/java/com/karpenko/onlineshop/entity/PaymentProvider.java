package com.karpenko.onlineshop.entity;

/**
 * Payment providers (gateways) available in the shop.
 * Display names and descriptions are localized via messages.properties
 * using keys: paymentProvider.{NAME}.name and paymentProvider.{NAME}.desc
 */
public enum PaymentProvider {
    STRIPE,
    VORKASSE,
    RECHNUNG
}