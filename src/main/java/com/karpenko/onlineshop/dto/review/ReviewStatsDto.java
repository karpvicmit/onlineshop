package com.karpenko.onlineshop.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReviewStatsDto {
    private Double averageRating;
    private Long totalCount;
}