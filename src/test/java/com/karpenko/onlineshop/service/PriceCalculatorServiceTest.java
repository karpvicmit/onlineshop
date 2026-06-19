package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import com.karpenko.onlineshop.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PriceCalculatorService - Cart Total Calculation")
class PriceCalculatorServiceTest {

    private PriceCalculatorService priceCalculatorService;

    @BeforeEach
    void setUp() {
        priceCalculatorService = new PriceCalculatorService();
    }

    @Test
    @DisplayName("Should calculate total for multiple items")
    void shouldCalculateTotalForMultipleItems() {
        Cart cart = new Cart();

        Product p1 = new Product();
        p1.setPrice(new BigDecimal("100.00"));
        CartItem item1 = new CartItem();
        item1.setProduct(p1);
        item1.setQuantity(2);
        cart.addItem(item1);

        Product p2 = new Product();
        p2.setPrice(new BigDecimal("50.50"));
        CartItem item2 = new CartItem();
        item2.setProduct(p2);
        item2.setQuantity(3);
        cart.addItem(item2);

        BigDecimal total = priceCalculatorService.calculateTotal(cart);

        // 100*2 + 50.50*3 = 200 + 151.50 = 351.50
        assertThat(total).isEqualByComparingTo("351.50");
    }

    @Test
    @DisplayName("Should return ZERO for empty cart")
    void shouldReturnZeroForEmptyCart() {
        Cart cart = new Cart();
        assertThat(priceCalculatorService.calculateTotal(cart)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should return ZERO for null cart")
    void shouldReturnZeroForNullCart() {
        assertThat(priceCalculatorService.calculateTotal(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}