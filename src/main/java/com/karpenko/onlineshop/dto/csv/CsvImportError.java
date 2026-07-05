package com.karpenko.onlineshop.dto.csv;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CsvImportError {
    private int rowNumber;
    private String sku;
    private String message;
}