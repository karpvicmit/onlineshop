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
import com.karpenko.onlineshop.service.FileUploadService;
import org.springframework.web.multipart.MultipartFile;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final FileUploadService fileUploadService;

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

    @Override
    @Transactional
    public Product saveProduct(Product product, MultipartFile imageFile) {
        log.info("Speichere Produkt: {}", product.getName());

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileUploadService.storeFile(imageFile);
            product.setImageUrl(imageUrl);
        } else if (product.getId() != null) {
            Product existingProduct = productRepository.findById(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produkt nicht gefunden"));
            product.setImageUrl(existingProduct.getImageUrl());
        }

        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Lösche Produkt mit ID: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Produkt nicht gefunden");
        }
        productRepository.deleteById(id);
    }
}