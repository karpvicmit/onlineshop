package com.karpenko.onlineshop.mapper;

import com.karpenko.onlineshop.dto.promo.PromoCodeDto;
import com.karpenko.onlineshop.entity.PromoCode;
import org.springframework.stereotype.Component;

@Component
public class PromoCodeMapper {

    public PromoCodeDto toDto(PromoCode promoCode) {
        if (promoCode == null) return null;
        return PromoCodeDto.builder()
                .id(promoCode.getId())
                .code(promoCode.getCode())
                .discountType(promoCode.getDiscountType())
                .discountValue(promoCode.getDiscountValue())
                .minOrderAmount(promoCode.getMinOrderAmount())
                .maxUses(promoCode.getMaxUses())
                .usedCount(promoCode.getUsedCount())
                .validFrom(promoCode.getValidFrom())
                .validUntil(promoCode.getValidUntil())
                .active(promoCode.getActive())
                .currentlyActive(promoCode.isActiveNow())
                .build();
    }
}