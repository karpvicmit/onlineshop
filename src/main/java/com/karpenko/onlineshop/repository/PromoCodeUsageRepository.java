package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.PromoCodeUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {

    List<PromoCodeUsage> findByUserIdOrderByUsedAtDesc(Long userId);

    List<PromoCodeUsage> findByPromoCodeIdOrderByUsedAtDesc(Long promoCodeId);
}