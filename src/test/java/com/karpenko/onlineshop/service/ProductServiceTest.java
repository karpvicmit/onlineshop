package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.exception.ResourceNotFoundException;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService - Soft Delete & Price Preservation")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private FileUploadService fileUploadService;

    @InjectMocks
    private ProductServiceImpl productService;

    @Nested
    @DisplayName("Soft Delete")
    class SoftDelete {

        @Test
        @DisplayName("Should mark product as deleted instead of removing from DB")
        void shouldSoftDeleteProduct() {
            Product product = new Product();
            product.setId(1L);
            product.setName("iPhone");
            product.setDeleted(false);
            when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));

            productService.deleteProduct(1L);

            assertThat(product.isDeleted()).isTrue();
            verify(productRepository, never()).delete(any(Product.class));
        }

        @Test
        @DisplayName("Should throw when trying to delete non-existent product")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findActiveById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Product not found");
        }

        @Test
        @DisplayName("Should not call delete() — only set flag")
        void shouldNotCallHardDelete() {
            Product product = new Product();
            product.setId(1L);
            product.setName("iPhone");
            product.setDeleted(false);
            when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));

            productService.deleteProduct(1L);

            verify(productRepository, never()).delete(any(Product.class));
            verify(productRepository, never()).deleteById(anyLong());
        }
    }
}