package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.mapper.ProductMapper;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.ProductService;
import com.karpenko.onlineshop.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductDto> findProducts(String name, String categorySlug, Pageable pageable) {
        log.debug("Suche Produkte: name='{}', category='{}', page={}", name, categorySlug, pageable.getPageNumber());

        Specification<Product> spec = ProductSpecification.hasNameAndCategory(name, categorySlug);

        Page<Product> productPage = productRepository.findAll(spec, pageable);

        return productPage.map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        log.debug("Abruf des Produkts mit ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Produkt mit ID {} nicht gefunden", id);
                    return new ResourceNotFoundException("Produkt nicht gefunden");
                });

        return productMapper.toDto(product);
    }
}