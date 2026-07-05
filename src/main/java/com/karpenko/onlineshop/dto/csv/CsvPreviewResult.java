package com.karpenko.onlineshop.dto.csv;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CsvPreviewResult {
    private int totalRows;
    private int toCreate;
    private int toUpdate;

    @Builder.Default
    private List<ProductCsvRow> sampleRows = new ArrayList<>();

    @Builder.Default
    private List<CsvImportError> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}