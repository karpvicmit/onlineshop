package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import org.springframework.stereotype.Service;
import com.karpenko.onlineshop.entity.Product;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Slf4j
@Service
public class PriceCalculatorService {
    
    public BigDecimal calculateTotal(Cart cart) {
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }

        return cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateTotalWithLockedPrices(
            List<CartItem> items,
            Map<Long, Product> lockedProducts) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return calculateTotalForItems(
                items,
                item -> {
                    Product locked = lockedProducts.get(item.getProduct().getId());
                    if (locked == null) {
                        log.error("Locked product not found for id: {}", item.getProduct().getId());
                        throw new IllegalStateException("Product price is not available");
                    }
                    return locked.getPrice();
                }
        );
    }

    /**
     * Generic calculator — the single source of truth for price × quantity summation.
     */
    private <T> BigDecimal calculateTotalForItems(
            List<T> items,
            Function<T, BigDecimal> priceExtractor) {
        return items.stream()
                .map(item -> priceExtractor.apply(item)
                        .multiply(BigDecimal.valueOf(getQuantity(item))))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private <T> int getQuantity(T item) {
        if (item instanceof CartItem cartItem) {
            return cartItem.getQuantity();
        }
        throw new IllegalArgumentException("Unsupported item type: " + item.getClass());
    }
}