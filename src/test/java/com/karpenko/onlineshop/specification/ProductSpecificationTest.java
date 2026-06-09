package com.karpenko.onlineshop.specification;

import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testet die dynamische Produktsuche via Specification (Szenario /T12).
 * @DataJpaTest lädt nur JPA-Komponenten (Repositories) mit H2-Datenbank.
 */
@DataJpaTest
class ProductSpecificationTest {

    @Autowired
    private ProductRepository productRepository;

    private Category electronics;
    private Category books;

    @BeforeEach
    void setUp() {
        electronics = new Category();
        electronics.setName("Elektronik");
        electronics.setSlug("elektronik");

        books = new Category();
        books.setName("Bücher");
        books.setSlug("buecher");

        Product laptop = new Product();
        laptop.setName("Gaming Laptop");
        laptop.setPrice(new BigDecimal("1500.00"));
        laptop.setCategory(electronics);

        Product phone = new Product();
        phone.setName("Smartphone");
        phone.setPrice(new BigDecimal("800.00"));
        phone.setCategory(electronics);

        Product javaBook = new Product();
        javaBook.setName("Java Programming");
        javaBook.setPrice(new BigDecimal("50.00"));
        javaBook.setCategory(books);

        productRepository.saveAll(List.of(laptop, phone, javaBook));
    }

    @Test
    void searchByName_IgnoreCase_ReturnsMatchingProducts() {
        // WHEN: Suche nach "lap" (case-insensitive)
        Specification<Product> spec = ProductSpecification.hasNameAndCategory("lap", null);
        List<Product> results = productRepository.findAll(spec);

        // THEN: Nur der Laptop wird gefunden
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    void searchByCategory_ReturnsAllProductsInCategory() {
        // WHEN: Filter nach Kategorie "elektronik"
        Specification<Product> spec = ProductSpecification.hasNameAndCategory(null, "elektronik");
        List<Product> results = productRepository.findAll(spec);

        // THEN: Laptop und Smartphone werden gefunden
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::getName).containsExactlyInAnyOrder("Gaming Laptop", "Smartphone");
    }
}