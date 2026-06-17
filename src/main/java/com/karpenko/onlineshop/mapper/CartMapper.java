package com.karpenko.onlineshop.mapper;

import com.karpenko.onlineshop.dto.CartDto;
import com.karpenko.onlineshop.dto.CartItemDto;
import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Manueller Mapper für Cart → CartDto.
 * Einfache native Java-Lösung ohne MapStruct.
 */
@Component
public class CartMapper {

    public CartDto toDto(Cart cart, BigDecimal total) {
        List<CartItemDto> items = cart.getItems().stream()
                .map(this::toItemDto)
                .toList();

        int itemCount = items.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();

        return CartDto.builder()
                .id(cart.getId())
                .items(items)
                .total(total)
                .itemCount(itemCount)
                .build();
    }

    private CartItemDto toItemDto(CartItem item) {
        BigDecimal subtotal = item.getProduct().getPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));

        return CartItemDto.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(item.getProduct().getImageUrl())
                .unitPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .subtotal(subtotal)
                .build();
    }
}