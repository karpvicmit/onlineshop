package com.karpenko.onlineshop.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlyRevenueDto {
    private Integer year;
    private Integer month;
    private BigDecimal totalRevenue;
}