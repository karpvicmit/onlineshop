package com.karpenko.onlineshop.dto.product;

import lombok.Data;
import java.math.BigDecimal;


@Data
public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private String categoryName;
    private String categorySlug; 
}
