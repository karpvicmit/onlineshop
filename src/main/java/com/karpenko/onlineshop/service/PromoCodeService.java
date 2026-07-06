package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;

import java.math.BigDecimal;

public interface PromoCodeService {
    
    PromoCodeValidationResult validatePromoCode(String code, BigDecimal cartSubtotal);

    BigDecimal calculateDiscount(com.karpenko.onlineshop.entity.PromoCode promoCode,
                                 BigDecimal cartSubtotal);

    void incrementUsage(Long promoCodeId);

    void decrementUsage(Long promoCodeId);
}