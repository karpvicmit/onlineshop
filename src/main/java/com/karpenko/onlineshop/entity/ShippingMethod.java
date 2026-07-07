package com.karpenko.onlineshop.entity;

import java.math.BigDecimal;

public enum ShippingMethod {
    STANDARD("Standardversand", "Lieferung in 3–5 Werktagen", new BigDecimal("4.99")),
    EXPRESS("Expressversand", "Lieferung am nächsten Werktag", new BigDecimal("9.99")),
    ABHOLUNG("Selbstabholung", "Kostenlose Abholung im Lager", BigDecimal.ZERO);

    private final String displayName;
    private final String description;
    private final BigDecimal cost;

    ShippingMethod(String displayName, String description, BigDecimal cost) {
        this.displayName = displayName;
        this.description = description;
        this.cost = cost;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getCost() {
        return cost;
    }
}