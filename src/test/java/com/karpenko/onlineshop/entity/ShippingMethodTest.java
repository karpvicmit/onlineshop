package com.karpenko.onlineshop.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ShippingMethod - Display Names & Costs")
class ShippingMethodTest {

    @Test
    @DisplayName("STANDARD should cost 4.99 €")
    void standardShouldCost499() {
        assertThat(ShippingMethod.STANDARD.getCost()).isEqualByComparingTo("4.99");
        assertThat(ShippingMethod.STANDARD.getDisplayName()).isEqualTo("Standardversand");
    }

    @Test
    @DisplayName("EXPRESS should cost 9.99 €")
    void expressShouldCost999() {
        assertThat(ShippingMethod.EXPRESS.getCost()).isEqualByComparingTo("9.99");
        assertThat(ShippingMethod.EXPRESS.getDisplayName()).isEqualTo("Expressversand");
    }

    @Test
    @DisplayName("ABHOLUNG should be free (0.00 €)")
    void abholungShouldBeFree() {
        assertThat(ShippingMethod.ABHOLUNG.getCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ShippingMethod.ABHOLUNG.getDisplayName()).isEqualTo("Selbstabholung");
    }

    @Test
    @DisplayName("All shipping methods should have non-null display names and descriptions")
    void allMethodsShouldHaveDisplayNames() {
        for (ShippingMethod method : ShippingMethod.values()) {
            assertThat(method.getDisplayName()).isNotNull().isNotEmpty();
            assertThat(method.getDescription()).isNotNull().isNotEmpty();
            assertThat(method.getCost()).isNotNull();
        }
    }
}