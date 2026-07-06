package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.DiscountType;
import com.karpenko.onlineshop.entity.PromoCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PromoCodeRepository - JPQL Queries")
class PromoCodeRepositoryTest {

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @BeforeEach
    void setUp() {
        promoCodeRepository.deleteAll();
    }

    @Test
    @DisplayName("Should find active promo code by code (case-insensitive)")
    void shouldFindActiveByCode() {
        PromoCode promo = createPromo("SAVE10", true,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));
        promoCodeRepository.save(promo);

        Optional<PromoCode> result = promoCodeRepository.findActiveByCode(
                "SAVE10", LocalDateTime.now());

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("SAVE10");
    }

    @Test
    @DisplayName("Should not find inactive promo code")
    void shouldNotFindInactivePromo() {
        PromoCode promo = createPromo("SAVE10", false,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));
        promoCodeRepository.save(promo);

        Optional<PromoCode> result = promoCodeRepository.findActiveByCode(
                "SAVE10", LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not find expired promo code")
    void shouldNotFindExpiredPromo() {
        PromoCode promo = createPromo("SAVE10", true,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1));
        promoCodeRepository.save(promo);

        Optional<PromoCode> result = promoCodeRepository.findActiveByCode(
                "SAVE10", LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not find promo code not yet valid")
    void shouldNotFindNotYetValidPromo() {
        PromoCode promo = createPromo("SAVE10", true,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(30));
        promoCodeRepository.save(promo);

        Optional<PromoCode> result = promoCodeRepository.findActiveByCode(
                "SAVE10", LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find promo code with no expiry date")
    void shouldFindPromoWithNoExpiry() {
        PromoCode promo = createPromo("SAVE10", true,
                LocalDateTime.now().minusDays(1),
                null); // no expiry
        promoCodeRepository.save(promo);

        Optional<PromoCode> result = promoCodeRepository.findActiveByCode(
                "SAVE10", LocalDateTime.now());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should find all active promo codes")
    void shouldFindAllActive() {
        createAndSavePromo("SAVE10", true, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        createAndSavePromo("SAVE20", true, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        createAndSavePromo("INACTIVE", false, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30));
        createAndSavePromo("EXPIRED", true, LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));

        List<PromoCode> result = promoCodeRepository.findAllActive(LocalDateTime.now());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(PromoCode::getCode)
                .containsExactlyInAnyOrder("SAVE10", "SAVE20");
    }

    @Test
    @DisplayName("Should find promo code by code (case-insensitive)")
    void shouldFindByCodeIgnoreCase() {
        PromoCode promo = createPromo("SAVE10", true,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(30));
        promoCodeRepository.save(promo);

        Optional<PromoCode> result = promoCodeRepository.findByCodeIgnoreCase("save10");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("SAVE10");
    }

    private PromoCode createPromo(String code, boolean active,
                                  LocalDateTime validFrom, LocalDateTime validUntil) {
        PromoCode promo = new PromoCode();
        promo.setCode(code);
        promo.setDiscountType(DiscountType.PERCENTAGE);
        promo.setDiscountValue(new BigDecimal("10.00"));
        promo.setActive(active);
        promo.setValidFrom(validFrom);
        promo.setValidUntil(validUntil);
        promo.setUsedCount(0);
        return promo;
    }

    private void createAndSavePromo(String code, boolean active,
                                    LocalDateTime validFrom, LocalDateTime validUntil) {
        promoCodeRepository.save(createPromo(code, active, validFrom, validUntil));
    }
}