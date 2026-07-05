package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.csv.CsvImportError;
import com.karpenko.onlineshop.dto.csv.CsvImportResult;
import com.karpenko.onlineshop.dto.csv.CsvPreviewResult;
import com.karpenko.onlineshop.dto.csv.ProductCsvRow;
import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import com.karpenko.onlineshop.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final CsvParserService csvParserService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final int MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final int MAX_ROWS = 10_000;

    /**
     * Preview (dry-run): parses and validates the CSV without persisting anything.
     */
    public CsvPreviewResult preview(MultipartFile file) throws IOException {
        validateFile(file);

        List<String[]> rawRows = csvParserService.parse(file.getInputStream());
        if (rawRows.size() > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "Too many rows: " + rawRows.size() + ". Maximum allowed: " + MAX_ROWS);
        }

        List<CsvImportError> errors = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();
        Map<String, Integer> skuToExistingId = loadExistingSkuIndex();

        int toCreate = 0;
        int toUpdate = 0;
        List<ProductCsvRow> sample = new ArrayList<>();

        for (int i = 0; i < rawRows.size(); i++) {
            int rowNum = i + 2; // +2 because row 1 = header, i is 0-based
            ProductCsvRow row = csvParserService.toProductRow(rawRows.get(i));
            if (row == null) {
                errors.add(new CsvImportError(rowNum, null, "Malformed row"));
                continue;
            }

            List<String> rowErrors = validateRow(row);
            if (!rowErrors.isEmpty()) {
                errors.add(new CsvImportError(rowNum, row.getSku(), String.join("; ", rowErrors)));
                continue;
            }

            if (!seenSkus.add(row.getSku().toUpperCase())) {
                errors.add(new CsvImportError(rowNum, row.getSku(), "Duplicate SKU in file"));
                continue;
            }

            if (skuToExistingId.containsKey(row.getSku().toUpperCase())) {
                toUpdate++;
            } else {
                toCreate++;
            }

            if (sample.size() < 10) {
                sample.add(row);
            }
        }

        return CsvPreviewResult.builder()
                .totalRows(rawRows.size())
                .toCreate(toCreate)
                .toUpdate(toUpdate)
                .sampleRows(sample)
                .errors(errors)
                .build();
    }

    /**
     * Real import: persists products into the database (upsert by SKU).
     * Lenient mode — rows with errors are skipped, the rest are saved.
     */
    @Transactional
    public CsvImportResult importProducts(MultipartFile file) throws IOException {
        validateFile(file);

        List<String[]> rawRows = csvParserService.parse(file.getInputStream());
        if (rawRows.size() > MAX_ROWS) {
            throw new IllegalArgumentException(
                    "Too many rows: " + rawRows.size() + ". Maximum allowed: " + MAX_ROWS);
        }

        List<CsvImportError> errors = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();
        Map<String, Product> skuCache = new HashMap<>();
        Map<String, Category> categoryCache = loadCategoryCache();

        int created = 0, updated = 0, skipped = 0;

        for (int i = 0; i < rawRows.size(); i++) {
            int rowNum = i + 2;
            ProductCsvRow row = csvParserService.toProductRow(rawRows.get(i));
            if (row == null) {
                errors.add(new CsvImportError(rowNum, null, "Malformed row"));
                skipped++;
                continue;
            }

            List<String> rowErrors = validateRow(row);
            if (!rowErrors.isEmpty()) {
                errors.add(new CsvImportError(rowNum, row.getSku(), String.join("; ", rowErrors)));
                skipped++;
                continue;
            }

            if (!seenSkus.add(row.getSku().toUpperCase())) {
                errors.add(new CsvImportError(rowNum, row.getSku(), "Duplicate SKU in file"));
                skipped++;
                continue;
            }

            try {
                boolean isUpdate = persistRow(row, skuCache, categoryCache);
                if (isUpdate) updated++; else created++;
            } catch (Exception e) {
                log.warn("Failed to persist row {} (SKU={}): {}", rowNum, row.getSku(), e.getMessage());
                errors.add(new CsvImportError(rowNum, row.getSku(), "Persistence error: " + e.getMessage()));
                skipped++;
            }
        }

        log.info("CSV import finished: created={}, updated={}, skipped={}, errors={}",
                created, updated, skipped, errors.size());

        return CsvImportResult.builder()
                .totalRows(rawRows.size())
                .created(created)
                .updated(updated)
                .skipped(skipped)
                .errors(errors)
                .build();
    }

    // --- Private helpers ---

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Only .csv files are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "File too large. Maximum: 50 MB. Received: " + (file.getSize() / (1024 * 1024)) + " MB");
        }
    }

    private List<String> validateRow(ProductCsvRow row) {
        List<String> errs = new ArrayList<>();
        if (isBlank(row.getSku())) errs.add("sku is required");
        if (isBlank(row.getName())) errs.add("name is required");
        if (row.getPrice() == null) errs.add("price is required");
        else if (row.getPrice().compareTo(BigDecimal.ZERO) < 0) errs.add("price must be >= 0");
        if (row.getStock() == null) errs.add("stock is required");
        else if (row.getStock() < 0) errs.add("stock must be >= 0");
        if (isBlank(row.getCategorySlug())) errs.add("categorySlug is required");
        return errs;
    }

    private boolean persistRow(ProductCsvRow row,
                               Map<String, Product> skuCache,
                               Map<String, Category> categoryCache) {

        String skuKey = row.getSku().toUpperCase();
        Category category = resolveCategory(row.getCategorySlug(), categoryCache);

        Product product = skuCache.computeIfAbsent(skuKey, k ->
                productRepository.findBySku(row.getSku()).orElse(null));

        boolean isUpdate = product != null;
        if (!isUpdate) {
            product = new Product();
            product.setSku(row.getSku());
        }

        product.setName(row.getName());
        product.setDescription(row.getDescription());
        product.setPrice(row.getPrice());
        product.setStock(row.getStock());
        product.setCategory(category);
        product.setImageUrl(row.getImageUrl());
        product.setDeleted(false);

        Product saved = productRepository.save(product);
        skuCache.put(skuKey, saved);
        return isUpdate;
    }

    private Category resolveCategory(String slug, Map<String, Category> cache) {
        String key = slug.toLowerCase();
        return cache.computeIfAbsent(key, k -> {
            Optional<Category> existing = categoryRepository.findBySlug(key);
            if (existing.isPresent()) {
                return existing.get();
            }
            // Auto-create category if not found
            Category newCat = new Category();
            newCat.setName(slug);
            newCat.setSlug(SlugUtil.generateSlug(slug));
            log.info("Auto-creating missing category: {}", slug);
            return categoryRepository.save(newCat);
        });
    }

    private Map<String, Category> loadCategoryCache() {
        Map<String, Category> map = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            map.put(c.getSlug().toLowerCase(), c);
        }
        return map;
    }

    private Map<String, Integer> loadExistingSkuIndex() {
        Map<String, Integer> map = new HashMap<>();
        for (Product p : productRepository.findAll()) {
            if (p.getSku() != null) {
                map.put(p.getSku().toUpperCase(), p.getId().intValue());
            }
        }
        return map;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}