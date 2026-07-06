package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.promo.PromoCodeValidationResult;
import com.karpenko.onlineshop.entity.DiscountType;
import com.karpenko.onlineshop.entity.PromoCode;
import com.karpenko.onlineshop.repository.PromoCodeRepository;
import com.karpenko.onlineshop.service.impl.PromoCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromoCodeService - Validation & Discount Calculation")
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoCodeServiceImpl promoCodeService;

    private PromoCode percentagePromo;
    private PromoCode fixedPromo;

    @BeforeEach
    void setUp() {
        percentagePromo = new PromoCode();
        percentagePromo.setId(1L);
        percentagePromo.setCode("SAVE10");
        percentagePromo.setDiscountType(DiscountType.PERCENTAGE);
        percentagePromo.setDiscountValue(new BigDecimal("10.00"));
        percentagePromo.setValidFrom(LocalDateTime.now().minusDays(1));
        percentagePromo.setValidUntil(LocalDateTime.now().plusDays(30));
        percentagePromo.setActive(true);
        percentagePromo.setUsedCount(0);
        percentagePromo.setMaxUses(100);

        fixedPromo = new PromoCode();
        fixedPromo.setId(2L);
        fixedPromo.setCode("FIXED20");
        fixedPromo.setDiscountType(DiscountType.FIXED);
        fixedPromo.setDiscountValue(new BigDecimal("20.00"));
        fixedPromo.setValidFrom(LocalDateTime.now().minusDays(1));
        fixedPromo.setValidUntil(null); // no expiry
        fixedPromo.setActive(true);
        fixedPromo.setUsedCount(0);
        fixedPromo.setMaxUses(null); // unlimited
    }

    @Nested
    @DisplayName("validatePromoCode()")
    class ValidatePromoCode {

        @Test
        @DisplayName("Should validate valid percentage promo code")
        void shouldValidatePercentagePromo() {
            when(promoCodeRepository.findActiveByCode(eq("SAVE10"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentagePromo));

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "SAVE10", new BigDecimal("100.00"));

            assertThat(result.isValid()).isTrue();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("10.00"); // 10% of 100
            assertThat(result.getFinalTotal()).isEqualByComparingTo("90.00");
            assertThat(result.getPromoCode()).isEqualTo(percentagePromo);
        }

        @Test
        @DisplayName("Should validate valid fixed discount promo code")
        void shouldValidateFixedPromo() {
            when(promoCodeRepository.findActiveByCode(eq("FIXED20"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(fixedPromo));

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "FIXED20", new BigDecimal("100.00"));

            assertThat(result.isValid()).isTrue();
            assertThat(result.getDiscountAmount()).isEqualByComparingTo("20.00");
            assertThat(result.getFinalTotal()).isEqualByComparingTo("80.00");
        }

        @Test
        @DisplayName("Should normalize promo code to uppercase")
        void shouldNormalizeToUppercase() {
            when(promoCodeRepository.findActiveByCode(eq("SAVE10"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentagePromo));

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "save10", new BigDecimal("100.00"));

            assertThat(result.isValid()).isTrue();
            verify(promoCodeRepository).findActiveByCode(eq("SAVE10"), any());
        }

        @Test
        @DisplayName("Should return failure for invalid promo code")
        void shouldReturnFailureForInvalidCode() {
            when(promoCodeRepository.findActiveByCode(eq("INVALID"), any(LocalDateTime.class)))
                    .thenReturn(Optional.empty());

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "INVALID", new BigDecimal("100.00"));

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Invalid or expired");
        }

        @Test
        @DisplayName("Should return failure for blank promo code")
        void shouldReturnFailureForBlankCode() {
            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "  ", new BigDecimal("100.00"));

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("required");
        }

        @Test
        @DisplayName("Should return failure when usage limit reached")
        void shouldReturnFailureWhenUsageLimitReached() {
            percentagePromo.setUsedCount(100);
            percentagePromo.setMaxUses(100);

            when(promoCodeRepository.findActiveByCode(eq("SAVE10"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentagePromo));

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "SAVE10", new BigDecimal("100.00"));

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("usage limit");
        }

        @Test
        @DisplayName("Should return failure when min order amount not met")
        void shouldReturnFailureWhenMinOrderNotMet() {
            percentagePromo.setMinOrderAmount(new BigDecimal("50.00"));

            when(promoCodeRepository.findActiveByCode(eq("SAVE10"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentagePromo));

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "SAVE10", new BigDecimal("30.00"));

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("50.00");
        }

        @Test
        @DisplayName("Should validate when min order amount is met")
        void shouldValidateWhenMinOrderMet() {
            percentagePromo.setMinOrderAmount(new BigDecimal("50.00"));

            when(promoCodeRepository.findActiveByCode(eq("SAVE10"), any(LocalDateTime.class)))
                    .thenReturn(Optional.of(percentagePromo));

            PromoCodeValidationResult result = promoCodeService.validatePromoCode(
                    "SAVE10", new BigDecimal("100.00"));

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("calculateDiscount()")
    class CalculateDiscount {

        @Test
        @DisplayName("Should calculate percentage discount correctly")
        void shouldCalculatePercentageDiscount() {
            BigDecimal discount = promoCodeService.calculateDiscount(
                    percentagePromo, new BigDecimal("200.00"));

            assertThat(discount).isEqualByComparingTo("20.00"); // 10% of 200
        }

        @Test
        @DisplayName("Should calculate fixed discount correctly")
        void shouldCalculateFixedDiscount() {
            BigDecimal discount = promoCodeService.calculateDiscount(
                    fixedPromo, new BigDecimal("100.00"));

            assertThat(discount).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("Should cap fixed discount at cart subtotal")
        void shouldCapFixedDiscountAtSubtotal() {
            fixedPromo.setDiscountValue(new BigDecimal("150.00"));

            BigDecimal discount = promoCodeService.calculateDiscount(
                    fixedPromo, new BigDecimal("100.00"));

            assertThat(discount).isEqualByComparingTo("100.00"); // capped at subtotal
        }

        @Test
        @DisplayName("Should cap percentage discount at 100%")
        void shouldCapPercentageAt100() {
            percentagePromo.setDiscountValue(new BigDecimal("150.00")); // > 100%

            BigDecimal discount = promoCodeService.calculateDiscount(
                    percentagePromo, new BigDecimal("100.00"));

            assertThat(discount).isEqualByComparingTo("100.00"); // capped at 100%
        }

        @Test
        @DisplayName("Should return ZERO for null promo code")
        void shouldReturnZeroForNullPromo() {
            BigDecimal discount = promoCodeService.calculateDiscount(
                    null, new BigDecimal("100.00"));

            assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should return ZERO for null subtotal")
        void shouldReturnZeroForNullSubtotal() {
            BigDecimal discount = promoCodeService.calculateDiscount(
                    percentagePromo, null);

            assertThat(discount).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("incrementUsage() / decrementUsage()")
    class UsageTracking {

        @Test
        @DisplayName("Should increment usedCount")
        void shouldIncrementUsage() {
            percentagePromo.setUsedCount(5);
            when(promoCodeRepository.findById(1L)).thenReturn(Optional.of(percentagePromo));
            when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(inv -> inv.getArgument(0));

            promoCodeService.incrementUsage(1L);

            assertThat(percentagePromo.getUsedCount()).isEqualTo(6);
            verify(promoCodeRepository).save(percentagePromo);
        }

        @Test
        @DisplayName("Should decrement usedCount")
        void shouldDecrementUsage() {
            percentagePromo.setUsedCount(5);
            when(promoCodeRepository.findById(1L)).thenReturn(Optional.of(percentagePromo));
            when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(inv -> inv.getArgument(0));

            promoCodeService.decrementUsage(1L);

            assertThat(percentagePromo.getUsedCount()).isEqualTo(4);
            verify(promoCodeRepository).save(percentagePromo);
        }

        @Test
        @DisplayName("Should not decrement below zero")
        void shouldNotDecrementBelowZero() {
            percentagePromo.setUsedCount(0);
            when(promoCodeRepository.findById(1L)).thenReturn(Optional.of(percentagePromo));

            promoCodeService.decrementUsage(1L);

            assertThat(percentagePromo.getUsedCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should do nothing for null promoCodeId")
        void shouldDoNothingForNullId() {
            promoCodeService.incrementUsage(null);
            promoCodeService.decrementUsage(null);

            verify(promoCodeRepository, never()).findById(any());
        }
    }
}