package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;


public interface ProductService {
    
    Page<ProductDto> findProducts(String name, String categorySlug, Pageable pageable);
    ProductDto getProductById(Long id);

    Product saveProduct(Product product, MultipartFile imageFile);
    void deleteProduct(Long id);
}