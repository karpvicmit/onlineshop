package com.karpenko.onlineshop.mapper;

import com.karpenko.onlineshop.dto.CartDto;
import com.karpenko.onlineshop.dto.CartItemDto;
import com.karpenko.onlineshop.entity.Cart;
import com.karpenko.onlineshop.entity.CartItem;
import com.karpenko.onlineshop.service.PriceCalculatorService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public abstract class CartMapper {

    @Autowired
    protected PriceCalculatorService priceCalculatorService;

    @Mapping(target = "items", source = "cart.items")
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "itemCount", ignore = true)
    public abstract CartDto toDto(Cart cart);

    @AfterMapping
    protected void setTotalAndCount(Cart cart, @MappingTarget CartDto dto) {
        BigDecimal total = priceCalculatorService.calculateTotal(cart);
        dto.setTotal(total);
        int count = dto.getItems().stream()
                .mapToInt(CartItemDto::getQuantity)
                .sum();
        dto.setItemCount(count);
    }

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productImageUrl", source = "product.imageUrl")
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "subtotal", expression = "java(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))")
    public abstract CartItemDto toItemDto(CartItem item);
}