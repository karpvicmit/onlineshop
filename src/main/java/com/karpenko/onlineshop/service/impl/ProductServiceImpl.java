package com.karpenko.onlineshop.service.impl;

import com.karpenko.onlineshop.dto.product.ProductDto;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.mapper.ProductMapper;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.FileUploadService;
import com.karpenko.onlineshop.service.ProductService;
import com.karpenko.onlineshop.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        log.debug("Searching products: name='{}', category='{}', page={}", name, categorySlug, pageable.getPageNumber());

        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory(name, categorySlug))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        log.debug("getProductById: {}", id);
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.warn("Product with ID {} not found or deleted", id);
                    return new ResourceNotFoundException("Product not found");
                });
        return productMapper.toDto(product);
    }

    @Override
    @Transactional
    public Product getProductEntityById(Long id) {
        log.debug("getProductEntityById: {}", id);
        return productRepository.findActiveById(id)
                .orElseThrow(() -> {
                    log.warn("Product with ID {} not found or deleted", id);
                    return new ResourceNotFoundException("Product not found");
                });
    }

    @Override
    @Transactional
    public Product saveProduct(Product product, MultipartFile imageFile) {
        log.info("Saving product: {}", product.getName());

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileUploadService.storeFile(imageFile);
            product.setImageUrl(imageUrl);
        } else if (product.getId() != null) {
            Product existingProduct = productRepository.findActiveById(product.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            product.setImageUrl(existingProduct.getImageUrl());
            product.setVersion(existingProduct.getVersion());
        }
        product.setDeleted(false);
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with ID: {}", id);
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setDeleted(true);
        log.info("Product {} marked as deleted", product.getName());
    }
}