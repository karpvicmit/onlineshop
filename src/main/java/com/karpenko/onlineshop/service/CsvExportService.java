package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.csv.ProductCsvRow;
import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CsvParserService csvParserService;

    @Transactional(readOnly = true)
    public byte[] exportProducts() throws IOException {
        List<Product> products = productRepository.findAll();
        List<ProductCsvRow> rows = new ArrayList<>(products.size());

        for (Product p : products) {
            if (p.isDeleted()) continue;
            rows.add(ProductCsvRow.builder()
                    .sku(p.getSku())
                    .name(p.getName())
                    .description(p.getDescription())
                    .price(p.getPrice())
                    .stock(p.getStock())
                    .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                    .imageUrl(p.getImageUrl())
                    .build());
        }

        log.info("Exporting {} products to CSV", rows.size());
        return csvParserService.writeProductsCsv(rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportCategories() throws IOException {
        List<Category> categories = categoryRepository.findAll();
        List<String[]> rows = new ArrayList<>(categories.size());

        for (Category c : categories) {
            rows.add(new String[]{
                    c.getSlug(),
                    c.getName(),
                    c.getDescription() != null ? c.getDescription() : ""
            });
        }

        log.info("Exporting {} categories to CSV", rows.size());
        return csvParserService.writeCategoriesCsv(rows);
    }
}