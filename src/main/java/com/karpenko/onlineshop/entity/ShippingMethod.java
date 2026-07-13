package com.karpenko.onlineshop.entity;

import java.math.BigDecimal;

/**
 * Shipping methods available in the shop.
 * Display names and descriptions are localized via messages.properties
 * using keys: shipping.{NAME}.name and shipping.{NAME}.desc
 * Cost is stored here as it's a business constant, not a display text.
 */
public enum ShippingMethod {

    STANDARD(new BigDecimal("4.99")),
    EXPRESS(new BigDecimal("9.99")),
    ABHOLUNG(BigDecimal.ZERO);

    private final BigDecimal cost;

    ShippingMethod(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getCost() {
        return cost;
    }
}