package com.karpenko.onlineshop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final Path uploadDirectory;

    public FileUploadService(@Value("${app.upload-dir:./dev-uploads}") String uploadDir) {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDirectory);
            log.info("Upload-Verzeichnis erstellt oder verifiziert: {}", this.uploadDirectory);
        } catch (IOException e) {
            log.error("Konnte das Upload-Verzeichnis nicht erstellen: {}", this.uploadDirectory, e);
            throw new RuntimeException("Konnte das Upload-Verzeichnis nicht erstellen", e);
        }
    }

    /**
     * Speichert die Datei und gibt den relativen Pfad (für die DB) zurück.
     *
     * @param file Die hochzuladende Datei
     * @return Der relative Pfad zur Datei (z.B. "uploads/uuid-filename.jpg")
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Generiere einen eindeutigen Dateinamen, um Überschreibungen zu verhindern
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";
        String newFileName = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = this.uploadDirectory.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Datei erfolgreich gespeichert: {}", newFileName);

            // Rückgabe des relativen Pfades für die Datenbank
            return "/uploads/" + newFileName;
        } catch (IOException e) {
            log.error("Fehler beim Speichern der Datei: {}", newFileName, e);
            throw new RuntimeException("Fehler beim Speichern der Datei: " + newFileName, e);
        }
    }
}