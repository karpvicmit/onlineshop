package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;
import com.karpenko.onlineshop.entity.DiscountType;
import com.karpenko.onlineshop.entity.PromoCode;
import com.karpenko.onlineshop.repository.PromoCodeRepository;
import com.karpenko.onlineshop.service.PromoCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100");
    private static final int SCALE = 2;

    @Override
    @Transactional(readOnly = true)
    public PromoCodeValidationResult validatePromoCode(String code, BigDecimal cartSubtotal) {
        if (code == null || code.isBlank()) {
            return PromoCodeValidationResult.failure("Promo code is required");
        }

        String normalizedCode = code.trim().toUpperCase();
        LocalDateTime now = LocalDateTime.now();

        Optional<PromoCode> optional = promoCodeRepository
                .findActiveByCode(normalizedCode, now);

        if (optional.isEmpty()) {
            log.debug("Promo code not found or inactive: {}", normalizedCode);
            return PromoCodeValidationResult.failure(
                    "Invalid or expired promo code");
        }

        PromoCode promoCode = optional.get();

        if (promoCode.isUsageLimitReached()) {
            log.debug("Promo code usage limit reached: {}", normalizedCode);
            return PromoCodeValidationResult.failure(
                    "This promo code has reached its usage limit");
        }

        if (promoCode.getMinOrderAmount() != null
                && cartSubtotal.compareTo(promoCode.getMinOrderAmount()) < 0) {
            log.debug("Cart subtotal {} below minimum {} for promo code {}",
                    cartSubtotal, promoCode.getMinOrderAmount(), normalizedCode);
            return PromoCodeValidationResult.failure(
                    "Minimum order amount of " +
                            promoCode.getMinOrderAmount() + " € required");
        }

        BigDecimal discount = calculateDiscount(promoCode, cartSubtotal);
        log.info("Promo code {} validated. Discount: {} €", normalizedCode, discount);

        return PromoCodeValidationResult.success(promoCode, discount, cartSubtotal);
    }

    @Override
    public BigDecimal calculateDiscount(PromoCode promoCode, BigDecimal cartSubtotal) {
        if (promoCode == null || cartSubtotal == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if (promoCode.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal percentage = promoCode.getDiscountValue()
                    .min(MAX_PERCENTAGE); // cap at 100%
            discount = cartSubtotal
                    .multiply(percentage)
                    .divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP);
        } else {
            discount = promoCode.getDiscountValue().min(cartSubtotal);
        }

        return discount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public void incrementUsage(Long promoCodeId) {
        if (promoCodeId == null) return;
        promoCodeRepository.findById(promoCodeId).ifPresent(pc -> {
            pc.incrementUsedCount();
            promoCodeRepository.save(pc);
            log.info("Promo code {} usage incremented to {}", pc.getCode(), pc.getUsedCount());
        });
    }

    @Override
    @Transactional
    public void decrementUsage(Long promoCodeId) {
        if (promoCodeId == null) return;
        promoCodeRepository.findById(promoCodeId).ifPresent(pc -> {
            pc.decrementUsedCount();
            promoCodeRepository.save(pc);
            log.info("Promo code {} usage decremented to {}", pc.getCode(), pc.getUsedCount());
        });
    }
}