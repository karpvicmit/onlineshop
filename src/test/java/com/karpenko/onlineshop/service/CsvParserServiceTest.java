package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.csv.ProductCsvRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CsvParserService - CSV Parsing & Writing")
class CsvParserServiceTest {

    private CsvParserService csvParserService;

    @BeforeEach
    void setUp() {
        csvParserService = new CsvParserService();
    }

    @Nested
    @DisplayName("parse()")
    class Parse {

        @Test
        @DisplayName("Should parse valid CSV with semicolon separator")
        void shouldParseValidCsv() throws IOException {
            String csv = """
                    sku;name;description;price;stock;categorySlug;imageUrl
                    SKU-001;iPhone 15;New phone;999.00;10;elektronik;/uploads/iphone.jpg
                    SKU-002;Clean Code;Book;35.50;20;buecher;/uploads/code.jpg
                    """;
            List<String[]> rows = csvParserService.parse(toStream(csv));

            assertThat(rows).hasSize(2);
            assertThat(rows.get(0)[0]).isEqualTo("SKU-001");
            assertThat(rows.get(0)[1]).isEqualTo("iPhone 15");
            assertThat(rows.get(0)[3]).isEqualTo("999.00");
            assertThat(rows.get(1)[0]).isEqualTo("SKU-002");
        }

        @Test
        @DisplayName("Should skip header row")
        void shouldSkipHeader() throws IOException {
            String csv = """
                    sku;name;description;price;stock;categorySlug;imageUrl
                    SKU-001;iPhone;Desc;100;5;cat;/img.jpg
                    """;
            List<String[]> rows = csvParserService.parse(toStream(csv));

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[0]).isEqualTo("SKU-001");
        }

        @Test
        @DisplayName("Should handle UTF-8 BOM at the beginning")
        void shouldHandleUtf8Bom() throws IOException {
            byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            String csvContent = """
                    sku;name;description;price;stock;categorySlug;imageUrl
                    SKU-001;iPhone;Desc;100;5;cat;/img.jpg
                    """;
            byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);
            byte[] withBom = new byte[bom.length + csvBytes.length];
            System.arraycopy(bom, 0, withBom, 0, bom.length);
            System.arraycopy(csvBytes, 0, withBom, bom.length, csvBytes.length);

            List<String[]> rows = csvParserService.parse(new ByteArrayInputStream(withBom));

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[0]).isEqualTo("SKU-001");
        }

        @Test
        @DisplayName("Should parse file WITHOUT BOM correctly (no reset bug)")
        void shouldParseFileWithoutBom() throws IOException {
            String csv = """
                    sku;name;description;price;stock;categorySlug;imageUrl
                    SKU-001;iPhone;Desc;100;5;cat;/img.jpg
                    """;

            List<String[]> rows = csvParserService.parse(toStream(csv));

            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[1]).isEqualTo("iPhone");
        }

        @Test
        @DisplayName("Should return empty list for empty file")
        void shouldReturnEmptyForEmptyFile() throws IOException {
            String csv = "sku;name;description;price;stock;categorySlug;imageUrl\n";
            List<String[]> rows = csvParserService.parse(toStream(csv));
            assertThat(rows).isEmpty();
        }

        @Test
        @DisplayName("Should trim whitespace from values")
        void shouldTrimValues() throws IOException {
            String csv = """
                    sku;name;description;price;stock;categorySlug;imageUrl
                    SKU-001 ; iPhone ; Desc ; 100 ; 5 ; cat ; /img.jpg
                    """;
            List<String[]> rows = csvParserService.parse(toStream(csv));
            assertThat(rows.get(0)[0]).isEqualTo("SKU-001");
            assertThat(rows.get(0)[1]).isEqualTo("iPhone");
        }

        @Test
        @DisplayName("Should handle missing columns gracefully")
        void shouldHandleMissingColumns() throws IOException {
            String csv = """
                    sku;name;description;price;stock;categorySlug;imageUrl
                    SKU-001;iPhone
                    """;
            List<String[]> rows = csvParserService.parse(toStream(csv));
            assertThat(rows).hasSize(1);
            assertThat(rows.get(0)[0]).isEqualTo("SKU-001");
            assertThat(rows.get(0)[3]).isNull(); // price missing
        }

        private InputStream toStream(String content) {
            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    @DisplayName("toProductRow()")
    class ToProductRow {

        @Test
        @DisplayName("Should convert valid row to ProductCsvRow")
        void shouldConvertValidRow() {
            String[] tokens = {"SKU-001", "iPhone", "Desc", "999.99", "10", "elektronik", "/img.jpg"};
            ProductCsvRow row = csvParserService.toProductRow(tokens);

            assertThat(row.getSku()).isEqualTo("SKU-001");
            assertThat(row.getName()).isEqualTo("iPhone");
            assertThat(row.getPrice()).isEqualByComparingTo("999.99");
            assertThat(row.getStock()).isEqualTo(10);
            assertThat(row.getCategorySlug()).isEqualTo("elektronik");
        }

        @Test
        @DisplayName("Should parse price with comma as decimal separator")
        void shouldParseCommaAsDecimalSeparator() {
            String[] tokens = {"SKU-001", "iPhone", "Desc", "999,99", "10", "cat", "/img.jpg"};
            ProductCsvRow row = csvParserService.toProductRow(tokens);
            assertThat(row.getPrice()).isEqualByComparingTo("999.99");
        }

        @Test
        @DisplayName("Should return null for null input")
        void shouldReturnNullForNullInput() {
            assertThat(csvParserService.toProductRow(null)).isNull();
        }

        @Test
        @DisplayName("Should return null for too short row")
        void shouldReturnNullForShortRow() {
            assertThat(csvParserService.toProductRow(new String[]{"SKU"})).isNull();
        }

        @Test
        @DisplayName("Should handle invalid price gracefully (return null)")
        void shouldHandleInvalidPrice() {
            String[] tokens = {"SKU-001", "iPhone", "Desc", "not-a-number", "10", "cat", "/img.jpg"};
            ProductCsvRow row = csvParserService.toProductRow(tokens);
            assertThat(row.getPrice()).isNull();
        }
    }

    @Nested
    @DisplayName("writeProductsCsv()")
    class WriteProducts {

        @Test
        @DisplayName("Should write CSV with UTF-8 BOM and semicolon separator")
        void shouldWriteWithBomAndSemicolon() throws IOException {
            ProductCsvRow row = ProductCsvRow.builder()
                    .sku("SKU-001")
                    .name("iPhone")
                    .description("Phone")
                    .price(new BigDecimal("999.99"))
                    .stock(10)
                    .categorySlug("elektronik")
                    .imageUrl("/img.jpg")
                    .build();

            byte[] result = csvParserService.writeProductsCsv(List.of(row));

            assertThat(result[0]).isEqualTo((byte) 0xEF);
            assertThat(result[1]).isEqualTo((byte) 0xBB);
            assertThat(result[2]).isEqualTo((byte) 0xBF);

            String content = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);
            assertThat(content).contains("sku;name;description;price;stock;categorySlug;imageUrl");
            assertThat(content).contains("SKU-001;iPhone;Phone;999.99;10;elektronik;/img.jpg");
        }

        @Test
        @DisplayName("Should handle null price/stock as empty strings")
        void shouldHandleNullValues() throws IOException {
            ProductCsvRow row = ProductCsvRow.builder()
                    .sku("SKU-001").name("X").price(null).stock(null).categorySlug("cat").build();

            byte[] result = csvParserService.writeProductsCsv(List.of(row));
            String content = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);
            assertThat(content).contains("SKU-001;X;;");
        }
    }

    @Nested
    @DisplayName("writeCategoriesCsv()")
    class WriteCategories {

        @Test
        @DisplayName("Should write categories CSV with BOM")
        void shouldWriteCategoriesWithBom() throws IOException {
            List<String[]> rows = List.of(
                    new String[]{"elektronik", "Elektronik", "Devices"},
                    new String[]{"buecher", "Bücher", ""}
            );
            byte[] result = csvParserService.writeCategoriesCsv(rows);

            assertThat(result[0]).isEqualTo((byte) 0xEF);
            String content = new String(result, 3, result.length - 3, StandardCharsets.UTF_8);
            assertThat(content).contains("slug;name;description");
            assertThat(content).contains("elektronik;Elektronik;Devices");
            assertThat(content).contains("buecher;Bücher;");
        }
    }
}