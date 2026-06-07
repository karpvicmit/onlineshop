package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.product.ProductDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface ProductService {
    
    Page<ProductDto> findProducts(String name, String categorySlug, Pageable pageable);
    ProductDto getProductById(Long id);
}