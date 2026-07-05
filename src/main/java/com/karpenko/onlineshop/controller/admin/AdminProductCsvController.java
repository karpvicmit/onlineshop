package com.karpenko.onlineshop.controller.admin;

import com.karpenko.onlineshop.dto.csv.CsvImportResult;
import com.karpenko.onlineshop.dto.csv.CsvPreviewResult;
import com.karpenko.onlineshop.service.CsvExportService;
import com.karpenko.onlineshop.service.CsvImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductCsvController {

    private final CsvImportService csvImportService;
    private final CsvExportService csvExportService;

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportProducts() throws IOException {
        byte[] csv = csvExportService.exportProducts();
        String filename = "products_" + timestamp() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/categories/export")
    public ResponseEntity<byte[]> exportCategories() throws IOException {
        byte[] csv = csvExportService.exportCategories();
        String filename = "categories_" + timestamp() + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @PostMapping("/import/preview")
    public String previewImport(@RequestParam("csvFile") MultipartFile file,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        try {
            CsvPreviewResult preview = csvImportService.preview(file);
            model.addAttribute("preview", preview);
            model.addAttribute("previewFile", file.getOriginalFilename());
            return "admin/products/import-preview";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        } catch (IOException e) {
            log.error("Failed to read CSV file", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler beim Lesen der Datei.");
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/import")
    public String importProducts(@RequestParam("csvFile") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        try {
            CsvImportResult result = csvImportService.importProducts(file);

            String msg = String.format(
                    "Import abgeschlossen: %d erstellt, %d aktualisiert, %d übersprungen.",
                    result.getCreated(), result.getUpdated(), result.getSkipped());

            if (result.hasErrors()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        msg + " " + result.getErrors().size() + " Fehler.");
                redirectAttributes.addFlashAttribute("importErrors", result.getErrors());
            } else {
                redirectAttributes.addFlashAttribute("successMessage", msg);
            }

            return "redirect:/admin/products";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        } catch (IOException e) {
            log.error("Failed to import CSV", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Fehler beim Import.");
            return "redirect:/admin/products";
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
    }
}