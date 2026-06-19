package com.karpenko.onlineshop.mapper;

import com.karpenko.onlineshop.dto.CartDto;
import com.karpenko.onlineshop.dto.CartItemDto;
import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import com.karpenko.onlineshop.service.PriceCalculatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {

    private final PriceCalculatorService priceCalculatorService;

    public CartDto toDto(Cart cart) {
        if (cart == null) {
            return CartDto.builder()
                    .items(Collections.emptyList())
                    .total(BigDecimal.ZERO)
                    .itemCount(0)
                    .build();
        }

        List<CartItemDto> itemDtos = cart.getItems() == null
                ? Collections.emptyList()
                : cart.getItems().stream()
                  .map(this::toItemDto)
                  .toList();

        BigDecimal total = priceCalculatorService.calculateTotal(cart);
        int itemCount = itemDtos.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();

        return CartDto.builder()
                .id(cart.getId())
                .items(itemDtos)
                .total(total)
                .itemCount(itemCount)
                .build();
    }

    public CartItemDto toItemDto(CartItem item) {
        if (item == null || item.getProduct() == null) {
            return null;
        }

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