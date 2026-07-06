package com.karpenko.onlineshop.mapper;

import com.karpenko.onlineshop.dto.cart.CartDto;
import com.karpenko.onlineshop.dto.cart.CartItemDto;
import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;
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
        return toDto(cart, null, null);
    }

    public CartDto toDto(Cart cart,
                         PromoCodeValidationResult promoResult,
                         String promoErrorMessage) {
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

        BigDecimal subtotal = priceCalculatorService.calculateTotal(cart);
        int itemCount = itemDtos.stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();

        CartDto.CartDtoBuilder builder = CartDto.builder()
                .id(cart.getId())
                .items(itemDtos)
                .total(subtotal)
                .itemCount(itemCount);

        if (promoResult != null && promoResult.isValid()) {
            builder.appliedPromoCode(promoResult.getPromoCode().getCode());
            builder.discountAmount(promoResult.getDiscountAmount());
            builder.finalTotal(promoResult.getFinalTotal());
        } else {
            builder.finalTotal(subtotal);
        }

        if (promoErrorMessage != null) {
            builder.promoErrorMessage(promoErrorMessage);
        }

        return builder.build();
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