package com.karpenko.onlineshop.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShippingMethod - Costs")
class ShippingMethodTest {

    @Test
    @DisplayName("STANDARD should cost 4.99 €")
    void standardShouldCost499() {
        assertThat(ShippingMethod.STANDARD.getCost()).isEqualByComparingTo("4.99");
    }

    @Test
    @DisplayName("EXPRESS should cost 9.99 €")
    void expressShouldCost999() {
        assertThat(ShippingMethod.EXPRESS.getCost()).isEqualByComparingTo("9.99");
    }

    @Test
    @DisplayName("ABHOLUNG should be free (0.00 €)")
    void abholungShouldBeFree() {
        assertThat(ShippingMethod.ABHOLUNG.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("All shipping methods should have non-null cost")
    void allMethodsShouldHaveCost() {
        for (ShippingMethod method : ShippingMethod.values()) {
            // Display names and descriptions are now localized via messages.properties,
            // so we only verify that the cost (business constant) is present.
            assertThat(method.getCost()).isNotNull();
        }
    }
}