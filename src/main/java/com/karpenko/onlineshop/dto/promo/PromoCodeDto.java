package com.karpenko.onlineshop.dto.promo;

import com.karpenko.onlineshop.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeDto {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderAmount;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
    private boolean currentlyActive;
    private Integer remainingUses;

    public Integer getRemainingUses() {
        if (maxUses == null) return null;
        return Math.max(0, maxUses - usedCount);
    }
}