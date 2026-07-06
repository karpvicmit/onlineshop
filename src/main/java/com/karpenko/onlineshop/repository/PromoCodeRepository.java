package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    @Query("SELECT p FROM PromoCode p WHERE p.code = :code " +
            "AND p.active = true " +
            "AND p.validFrom <= :now " +
            "AND (p.validUntil IS NULL OR p.validUntil > :now)")
    Optional<PromoCode> findActiveByCode(@Param("code") String code, @Param("now") LocalDateTime now);

    @Query("SELECT p FROM PromoCode p WHERE p.active = true " +
            "AND p.validFrom <= :now " +
            "AND (p.validUntil IS NULL OR p.validUntil > :now) " +
            "ORDER BY p.createdAt DESC")
    List<PromoCode> findAllActive(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM PromoCodeUsage u WHERE u.promoCode.id = :promoCodeId AND u.user.id = :userId")
    long countUsagesByUser(@Param("promoCodeId") Long promoCodeId, @Param("userId") Long userId);
}