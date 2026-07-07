package com.karpenko.onlineshop.entity;

public enum PaymentMethod {
    VORKASSE("Vorkasse", "Bezahlung vor Versand per Überweisung"),
    RECHNUNG("Rechnung", "Bezahlung innerhalb von 14 Tagen nach Erhalt"),
    KREDITKARTE("Kreditkarte", "Sichere Zahlung per Kreditkarte (Visa, Mastercard)"),
    PAYPAL("PayPal", "Bezahlung über Ihr PayPal-Konto"),
    SOFORT("Sofortüberweisung", "Direkte Banküberweisung via Sofort");

    private final String displayName;
    private final String description;

    PaymentMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}