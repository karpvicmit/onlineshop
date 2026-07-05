package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.csv.CsvImportResult;
import com.karpenko.onlineshop.dto.csv.CsvPreviewResult;
import com.karpenko.onlineshop.entity.Category;
import com.karpenko.onlineshop.entity.Product;
import com.karpenko.onlineshop.repository.CategoryRepository;
import com.karpenko.onlineshop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CsvImportService - Import & Preview Logic")
class CsvImportServiceTest {

    @Mock
    private CsvParserService csvParserService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CsvImportService csvImportService;

    private MockMultipartFile validCsvFile;

    @BeforeEach
    void setUp() {
        validCsvFile = new MockMultipartFile(
                "csvFile", "products.csv", "text/csv", "content".getBytes());
    }

    @Nested
    @DisplayName("File validation")
    class FileValidation {

        @Test
        @DisplayName("Should reject empty file")
        void shouldRejectEmptyFile() {
            MockMultipartFile empty = new MockMultipartFile(
                    "csvFile", "empty.csv", "text/csv", new byte[0]);
            assertThatThrownBy(() -> csvImportService.preview(empty))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("Should reject non-CSV file")
        void shouldRejectNonCsvFile() {
            MockMultipartFile txt = new MockMultipartFile(
                    "csvFile", "data.txt", "text/plain", "content".getBytes());
            assertThatThrownBy(() -> csvImportService.preview(txt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(".csv");
        }

        @Test
        @DisplayName("Should reject null file")
        void shouldRejectNullFile() {
            assertThatThrownBy(() -> csvImportService.preview(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("preview()")
    class Preview {

        @Test
        @DisplayName("Should count new and existing products correctly")
        void shouldCountNewAndExisting() throws IOException {
            String[][] rawRows = {
                    {"SKU-NEW", "New Product", "Desc", "100", "10", "cat", "/img.jpg"},
                    {"SKU-EXIST", "Existing", "Desc", "200", "5", "cat", "/img.jpg"}
            };

            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildRow("SKU-NEW", "New Product"));
            when(csvParserService.toProductRow(rawRows[1]))
                    .thenReturn(buildRow("SKU-EXIST", "Existing"));

            Product existing = new Product();
            existing.setId(1L);
            existing.setSku("SKU-EXIST");
            when(productRepository.findAll()).thenReturn(List.of(existing));

            CsvPreviewResult result = csvImportService.preview(validCsvFile);

            assertThat(result.getTotalRows()).isEqualTo(2);
            assertThat(result.getToCreate()).isEqualTo(1);
            assertThat(result.getToUpdate()).isEqualTo(1);
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should detect duplicate SKU within file")
        void shouldDetectDuplicateSkuInFile() throws IOException {
            String[][] rawRows = {
                    {"SKU-001", "P1", "Desc", "100", "10", "cat", "/img.jpg"},
                    {"SKU-001", "P1-dup", "Desc", "200", "5", "cat", "/img.jpg"}
            };
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildRow("SKU-001", "P1"));
            when(csvParserService.toProductRow(rawRows[1]))
                    .thenReturn(buildRow("SKU-001", "P1-dup"));
            when(productRepository.findAll()).thenReturn(List.of());

            CsvPreviewResult result = csvImportService.preview(validCsvFile);

            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0).getMessage()).contains("Duplicate SKU");
        }

        @Test
        @DisplayName("Should report validation errors for invalid rows")
        void shouldReportValidationErrors() throws IOException {
            String[][] rawRows = {
                    {"", "", "Desc", null, null, "", "/img.jpg"} // all required fields missing
            };
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildRow(null, null));
            when(productRepository.findAll()).thenReturn(List.of());

            CsvPreviewResult result = csvImportService.preview(validCsvFile);

            assertThat(result.getErrors()).isNotEmpty();
            assertThat(result.getErrors().get(0).getMessage()).contains("sku is required");
        }

        @Test
        @DisplayName("Should return sample of first 10 rows")
        void shouldReturnSampleRows() throws IOException {
            String[][] rawRows = new String[15][];
            for (int i = 0; i < 15; i++) {
                rawRows[i] = new String[]{"SKU-" + i, "P" + i, "Desc", "100", "10", "cat", "/img.jpg"};
                when(csvParserService.toProductRow(rawRows[i]))
                        .thenReturn(buildRow("SKU-" + i, "P" + i));
            }
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(productRepository.findAll()).thenReturn(List.of());

            CsvPreviewResult result = csvImportService.preview(validCsvFile);

            assertThat(result.getSampleRows()).hasSize(10);
        }
    }

    @Nested
    @DisplayName("importProducts()")
    class Import {

        @Test
        @DisplayName("Should create new product when SKU does not exist")
        void shouldCreateNewProduct() throws IOException {
            String[][] rawRows = {{"SKU-NEW", "iPhone", "Desc", "999", "10", "elektronik", "/img.jpg"}};
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildFullRow("SKU-NEW", "iPhone", "elektronik"));

            when(productRepository.findBySku("SKU-NEW")).thenReturn(Optional.empty());

            Category cat = new Category();
            cat.setSlug("elektronik");
            when(categoryRepository.findAll()).thenReturn(List.of(cat));

            when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });

            CsvImportResult result = csvImportService.importProducts(validCsvFile);

            assertThat(result.getCreated()).isEqualTo(1);
            assertThat(result.getUpdated()).isZero();

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            assertThat(captor.getValue().getSku()).isEqualTo("SKU-NEW");
            assertThat(captor.getValue().getName()).isEqualTo("iPhone");
        }

        @Test
        @DisplayName("Should update existing product when SKU exists")
        void shouldUpdateExistingProduct() throws IOException {
            String[][] rawRows = {{"SKU-EXIST", "Updated Name", "New Desc", "500", "20", "cat", "/img.jpg"}};
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildFullRow("SKU-EXIST", "Updated Name", "cat"));

            Product existing = new Product();
            existing.setId(1L);
            existing.setSku("SKU-EXIST");
            existing.setName("Old Name");
            when(productRepository.findBySku("SKU-EXIST")).thenReturn(Optional.of(existing));

            Category cat = new Category();
            cat.setSlug("cat");
            when(categoryRepository.findAll()).thenReturn(List.of(cat));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CsvImportResult result = csvImportService.importProducts(validCsvFile);

            assertThat(result.getUpdated()).isEqualTo(1);
            assertThat(result.getCreated()).isZero();
            assertThat(existing.getName()).isEqualTo("Updated Name");
        }

        @Test
        @DisplayName("Should auto-create missing category")
        void shouldAutoCreateMissingCategory() throws IOException {
            String[][] rawRows = {{"SKU-001", "P", "Desc", "100", "10", "new-cat", "/img.jpg"}};
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildFullRow("SKU-001", "P", "new-cat"));

            when(productRepository.findBySku("SKU-001")).thenReturn(Optional.empty());
            when(categoryRepository.findAll()).thenReturn(List.of());
            when(categoryRepository.findBySlug("new-cat")).thenReturn(Optional.empty());
            when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(1L);
                return c;
            });
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            csvImportService.importProducts(validCsvFile);

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).save(captor.capture());
            assertThat(captor.getValue().getSlug()).isEqualTo("new-cat");
        }

        @Test
        @DisplayName("Should skip invalid rows and continue import")
        void shouldSkipInvalidRows() throws IOException {
            String[][] rawRows = {
                    {"", "", "", null, null, "", ""}, // invalid
                    {"SKU-OK", "Good", "Desc", "100", "10", "cat", "/img.jpg"} // valid
            };
            when(csvParserService.parse(any())).thenReturn(List.of(rawRows));
            when(csvParserService.toProductRow(rawRows[0]))
                    .thenReturn(buildRow(null, null));
            when(csvParserService.toProductRow(rawRows[1]))
                    .thenReturn(buildFullRow("SKU-OK", "Good", "cat"));

            when(productRepository.findBySku("SKU-OK")).thenReturn(Optional.empty());
            Category cat = new Category();
            cat.setSlug("cat");
            when(categoryRepository.findAll()).thenReturn(List.of(cat));
            when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CsvImportResult result = csvImportService.importProducts(validCsvFile);

            assertThat(result.getSkipped()).isEqualTo(1);
            assertThat(result.getCreated()).isEqualTo(1);
            assertThat(result.getErrors()).hasSize(1);
        }
    }

    // ==========================================
    // Helpers
    // ==========================================
    private com.karpenko.onlineshop.dto.csv.ProductCsvRow buildRow(String sku, String name) {
        return com.karpenko.onlineshop.dto.csv.ProductCsvRow.builder()
                .sku(sku)
                .name(name)
                .description("Desc")
                .price(new BigDecimal("100"))
                .stock(10)
                .categorySlug("cat")
                .imageUrl("/img.jpg")
                .build();
    }

    private com.karpenko.onlineshop.dto.csv.ProductCsvRow buildFullRow(String sku, String name, String catSlug) {
        return com.karpenko.onlineshop.dto.csv.ProductCsvRow.builder()
                .sku(sku)
                .name(name)
                .description("Desc")
                .price(new BigDecimal("100"))
                .stock(10)
                .categorySlug(catSlug)
                .imageUrl("/img.jpg")
                .build();
    }
}