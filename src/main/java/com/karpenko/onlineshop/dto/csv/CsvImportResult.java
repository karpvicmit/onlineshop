package com.karpenko.onlineshop.dto.csv;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class CsvImportResult {
    private int totalRows;
    private int created;
    private int updated;
    private int skipped;

    @Builder.Default
    private List<CsvImportError> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}