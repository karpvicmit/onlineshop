package com.karpenko.onlineshop.dto.csv;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCsvRow {
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String categorySlug;
    private String imageUrl;
}