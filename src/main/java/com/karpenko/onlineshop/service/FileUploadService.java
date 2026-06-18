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
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private final Path uploadDirectory;
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    public FileUploadService(@Value("${app.upload-dir:./dev-uploads}") String uploadDir) {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDirectory);
            log.info("Upload directory created/verified: {}", this.uploadDirectory);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", this.uploadDirectory, e);
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.error("File exceeds max size ({} MB)", MAX_FILE_SIZE / (1024 * 1024));
            throw new IllegalArgumentException("File too large. Maximum size: 5 MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            log.error("Invalid MIME type: {}", contentType);
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, GIF, WebP.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("File name is missing");
        }

        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFileName.substring(dotIndex).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.error("Invalid file extension: {}", extension);
            throw new IllegalArgumentException("Invalid file extension. Allowed: .jpg, .jpeg, .png, .gif, .webp");
        }

        String newFileName = UUID.randomUUID().toString() + extension;

        Path targetLocation = this.uploadDirectory.resolve(newFileName).normalize();
        if (!targetLocation.startsWith(this.uploadDirectory)) {
            log.error("Path traversal attempt detected: {}", newFileName);
            throw new SecurityException("Invalid file path");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved successfully: {}", newFileName);
            return "/uploads/" + newFileName;
        } catch (IOException e) {
            log.error("Failed to save file: {}", newFileName, e);
            throw new RuntimeException("Failed to save file", e);
        }
    }
}