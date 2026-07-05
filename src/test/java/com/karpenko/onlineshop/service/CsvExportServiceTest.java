package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsvExportService - Export Logic")
class CsvExportServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CsvParserService csvParserService;

    @InjectMocks
    private CsvExportService csvExportService;

    @Test
    @DisplayName("Should exclude soft-deleted products from export")
    void shouldExcludeDeletedProducts() throws IOException {
        Category cat = new Category();
        cat.setSlug("elektronik");

        Product active = buildProduct("SKU-001", "iPhone", cat, false);
        Product deleted = buildProduct("SKU-002", "OldPhone", cat, true);

        when(productRepository.findAll()).thenReturn(List.of(active, deleted));
        when(csvParserService.writeProductsCsv(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    var rows = (List<?>) inv.getArgument(0);
                    assertThat(rows).hasSize(1); // only active
                    return "csv-content".getBytes(StandardCharsets.UTF_8);
                });

        byte[] result = csvExportService.exportProducts();
        assertThat(result).isNotEmpty();
    }

    @Test
    @DisplayName("Should export all non-deleted products")
    void shouldExportAllActiveProducts() throws IOException {
        Category cat = new Category();
        cat.setSlug("cat");

        when(productRepository.findAll()).thenReturn(List.of(
                buildProduct("SKU-001", "P1", cat, false),
                buildProduct("SKU-002", "P2", cat, false)
        ));
        when(csvParserService.writeProductsCsv(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    var rows = (List<?>) inv.getArgument(0);
                    assertThat(rows).hasSize(2);
                    return new byte[0];
                });

        csvExportService.exportProducts();
    }

    @Test
    @DisplayName("Should export all categories")
    void shouldExportAllCategories() throws IOException {
        Category c1 = new Category();
        c1.setSlug("elektronik");
        c1.setName("Elektronik");
        c1.setDescription("Devices");

        Category c2 = new Category();
        c2.setSlug("buecher");
        c2.setName("Bücher");
        c2.setDescription(null);

        when(categoryRepository.findAll()).thenReturn(List.of(c1, c2));
        when(csvParserService.writeCategoriesCsv(org.mockito.ArgumentMatchers.anyList()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    var rows = (List<String[]>) inv.getArgument(0);
                    assertThat(rows).hasSize(2);
                    assertThat(rows.get(0)).containsExactly("elektronik", "Elektronik", "Devices");
                    assertThat(rows.get(1)).containsExactly("buecher", "Bücher", "");
                    return new byte[0];
                });

        csvExportService.exportCategories();
    }

    private Product buildProduct(String sku, String name, Category cat, boolean deleted) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setPrice(new BigDecimal("100.00"));
        p.setStock(10);
        p.setCategory(cat);
        p.setDeleted(deleted);
        return p;
    }
}