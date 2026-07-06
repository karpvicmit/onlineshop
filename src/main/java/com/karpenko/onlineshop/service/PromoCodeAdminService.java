package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.promo.PromoCodeDto;
import com.karpenko.onlineshop.entity.PromoCode;

import java.util.List;

public interface PromoCodeAdminService {

    List<PromoCodeDto> getAllPromoCodes();

    PromoCodeDto getPromoCodeById(Long id);

    PromoCode savePromoCode(PromoCode promoCode);

    void deletePromoCode(Long id);

    void toggleActive(Long id);
}