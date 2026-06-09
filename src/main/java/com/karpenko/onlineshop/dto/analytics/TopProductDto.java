package com.karpenko.onlineshop.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TopProductDto {
    private String productName;
    private Long totalQuantitySold;
}