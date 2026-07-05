package com.karpenko.onlineshop.service;

import com.karpenko.onlineshop.dto.csv.ProductCsvRow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV parser/writer using Apache Commons CSV.
 * German standard: semicolon separator, UTF-8 with BOM.
 */
@Slf4j
@Service
public class CsvParserService {

    public static final char SEPARATOR = ';';
    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public static final String[] PRODUCT_HEADERS = {
            "sku", "name", "description", "price", "stock", "categorySlug", "imageUrl"
    };

    /**
     * Parses a CSV InputStream into rows. Header is automatically skipped.
     */
    public List<String[]> parse(InputStream inputStream) throws IOException {
        List<String[]> rows = new ArrayList<>();

        // Wrap input stream to skip BOM if present
        InputStream bomAwareStream = new BOMInputStream(inputStream);

        try (Reader reader = new InputStreamReader(bomAwareStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setDelimiter(SEPARATOR)
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String[] row = new String[PRODUCT_HEADERS.length];
                for (int i = 0; i < PRODUCT_HEADERS.length; i++) {
                    try {
                        row[i] = record.isMapped(PRODUCT_HEADERS[i]) ? record.get(PRODUCT_HEADERS[i]) : null;
                    } catch (IllegalArgumentException e) {
                        row[i] = null;
                    }
                }
                rows.add(row);
            }
        }

        log.debug("Parsed {} CSV rows", rows.size());
        return rows;
    }

    /**
     * Converts a parsed row into ProductCsvRow.
     */
    public ProductCsvRow toProductRow(String[] tokens) {
        if (tokens == null || tokens.length < PRODUCT_HEADERS.length) {
            return null;
        }
        return ProductCsvRow.builder()
                .sku(safe(tokens, 0))
                .name(safe(tokens, 1))
                .description(safe(tokens, 2))
                .price(parseDecimal(safe(tokens, 3)))
                .stock(parseInt(safe(tokens, 4)))
                .categorySlug(safe(tokens, 5))
                .imageUrl(safe(tokens, 6))
                .build();
    }

    /**
     * Writes product rows to CSV as UTF-8 with BOM (Excel-compatible).
     */
    public byte[] writeProductsCsv(List<ProductCsvRow> rows) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(UTF8_BOM);

        try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setDelimiter(SEPARATOR)
                     .setHeader(PRODUCT_HEADERS)
                     .build())) {

            for (ProductCsvRow row : rows) {
                printer.printRecord(
                        row.getSku(),
                        row.getName(),
                        row.getDescription(),
                        row.getPrice() != null ? row.getPrice().toPlainString() : "",
                        row.getStock() != null ? row.getStock() : "",
                        row.getCategorySlug(),
                        row.getImageUrl()
                );
            }
        }

        log.debug("Exported {} products to CSV", rows.size());
        return baos.toByteArray();
    }

    /**
     * Writes category rows (slug;name;description) to CSV with BOM.
     */
    public byte[] writeCategoriesCsv(List<String[]> rows) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(UTF8_BOM);

        try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setDelimiter(SEPARATOR)
                     .setHeader("slug", "name", "description")
                     .build())) {

            for (String[] row : rows) {
                printer.printRecord(row[0], row[1], row[2]);
            }
        }

        log.debug("Exported {} categories to CSV", rows.size());
        return baos.toByteArray();
    }


    private String safe(String[] tokens, int index) {
        if (index >= tokens.length) return null;
        String v = tokens[index];
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null) return null;
        try {
            String normalized = value.replace(",", ".");
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parseInt(String value) {
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Simple BOM-aware input stream wrapper.
     * Skips UTF-8 BOM (EF BB BF) if present at the beginning.
     */
    // В классе CsvParserService заменить внутренний класс BOMInputStream на:

    private static class BOMInputStream extends FilterInputStream {
        private boolean bomSkipped = false;
        private final PushbackInputStream pushback;

        public BOMInputStream(InputStream in) {
            super(new PushbackInputStream(in, 3));
            this.pushback = (PushbackInputStream) this.in;
        }

        @Override
        public int read() throws IOException {
            if (!bomSkipped) skipBOM();
            return super.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!bomSkipped) skipBOM();
            return super.read(b, off, len);
        }

        private void skipBOM() throws IOException {
            byte[] bom = new byte[3];
            int bytesRead = pushback.read(bom, 0, 3);
            if (bytesRead == 3
                    && bom[0] == (byte) 0xEF
                    && bom[1] == (byte) 0xBB
                    && bom[2] == (byte) 0xBF) {
                log.debug("UTF-8 BOM detected and skipped");
                // BOM consumed — do nothing
            } else if (bytesRead > 0) {
                // Not a BOM — push bytes back into the stream
                pushback.unread(bom, 0, bytesRead);
            }
            bomSkipped = true;
        }
    }

}