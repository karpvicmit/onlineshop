package com.karpenko.onlineshop.repository;

import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.specification.ProductSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository - Search & Filter")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category electronics;
    private Category books;
    private int skuCounter = 1;

    @BeforeEach
    void setUp() {
        skuCounter = 1;
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        electronics = new Category();
        electronics.setName("Elektronik");
        electronics.setSlug("elektronik");
        electronics = categoryRepository.save(electronics);

        books = new Category();
        books.setName("Bücher");
        books.setSlug("buecher");
        books = categoryRepository.save(books);

        createProduct("iPhone 15", electronics, new BigDecimal("999.00"), 10);
        createProduct("MacBook Pro", electronics, new BigDecimal("1999.00"), 5);
        createProduct("Clean Code", books, new BigDecimal("35.50"), 20);
        createProduct("iPhone Case", electronics, new BigDecimal("19.99"), 50);
    }

    @Test
    @DisplayName("Should find products by name (substring search)")
    void shouldFindProductsByName() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory("iPhone", null))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
                .extracting(Product::getName)
                .containsExactlyInAnyOrder("iPhone 15", "iPhone Case");
    }

    @Test
    @DisplayName("Should find products by category slug")
    void shouldFindProductsByCategory() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory(null, "buecher"))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should find products by name AND category")
    void shouldFindProductsByNameAndCategory() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory("iPhone", "elektronik"))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should return all products when no filters")
    void shouldReturnAllProductsWhenNoFilters() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory(null, null))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(4);
    }

    @Test
    @DisplayName("Should support pagination")
    void shouldSupportPagination() {
        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory(null, null))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> page0 = productRepository.findAll(spec, PageRequest.of(0, 2));
        Page<Product> page1 = productRepository.findAll(spec, PageRequest.of(1, 2));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page0.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should exclude soft-deleted products")
    void shouldExcludeDeletedProducts() {
        Product product = productRepository.findAll().get(0);
        product.setDeleted(true);
        productRepository.save(product);

        Specification<Product> spec = Specification
                .where(ProductSpecification.hasNameAndCategory(null, null))
                .and(ProductSpecification.isNotDeleted());

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Should find active product by ID")
    void shouldFindActiveProductById() {
        Product product = productRepository.findAll().get(0);

        var result = productRepository.findActiveById(product.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo(product.getName());
    }

    @Test
    @DisplayName("Should not find soft-deleted product by ID")
    void shouldNotFindDeletedProductById() {
        Product product = productRepository.findAll().get(0);
        product.setDeleted(true);
        productRepository.save(product);

        var result = productRepository.findActiveById(product.getId());

        assertThat(result).isEmpty();
    }

    private Product createProduct(String name, Category category, BigDecimal price, int stock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setStock(stock);
        product.setCategory(category);
        product.setSku("SKU-" + String.format("%04d", skuCounter++)); // <-- ДОБАВИТЬ
        return productRepository.save(product);
    }
}