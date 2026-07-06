package com.karpenko.onlineshop.dto.promo;

import com.karpenko.onlineshop.entity.DiscountType;
import com.karpenko.onlineshop.entity.PromoCode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PromoCodeValidationResult {
    private boolean valid;
    private String errorMessage;
    private PromoCode promoCode;
    private BigDecimal discountAmount;
    private DiscountType discountType;
    private BigDecimal cartSubtotal;
    private BigDecimal finalTotal;

    public static PromoCodeValidationResult success(PromoCode promoCode,
                                                    BigDecimal discountAmount,
                                                    BigDecimal cartSubtotal) {
        BigDecimal finalTotal = cartSubtotal.subtract(discountAmount)
                .max(BigDecimal.ZERO);
        return PromoCodeValidationResult.builder()
                .valid(true)
                .promoCode(promoCode)
                .discountAmount(discountAmount)
                .discountType(promoCode.getDiscountType())
                .cartSubtotal(cartSubtotal)
                .finalTotal(finalTotal)
                .build();
    }

    public static PromoCodeValidationResult failure(String errorMessage) {
        return PromoCodeValidationResult.builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }
}