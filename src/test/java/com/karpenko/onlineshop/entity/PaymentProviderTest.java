package com.karpenko.onlineshop.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentProvider - Enum Values")
class PaymentProviderTest {

    @Test
    @DisplayName("Should contain STRIPE, PAYPAL, VORKASSE, RECHNUNG")
    void shouldContainAllProviders() {
        PaymentProvider[] values = PaymentProvider.values();
        assertThat(values).containsExactlyInAnyOrder(
                PaymentProvider.STRIPE,
                PaymentProvider.VORKASSE,
                PaymentProvider.RECHNUNG
        );
    }

    @Test
    @DisplayName("Should be parseable from string")
    void shouldBeParseableFromString() {
        assertThat(PaymentProvider.valueOf("RECHNUNG")).isEqualTo(PaymentProvider.RECHNUNG);
        assertThat(PaymentProvider.valueOf("STRIPE")).isEqualTo(PaymentProvider.STRIPE);
        assertThat(PaymentProvider.valueOf("VORKASSE")).isEqualTo(PaymentProvider.VORKASSE);
    }
}