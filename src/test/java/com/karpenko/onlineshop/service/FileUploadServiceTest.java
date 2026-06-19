package com.karpenko.onlineshop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileUploadService - File Validation & Security")
class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    private FileUploadService fileUploadService;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(tempDir.toString());
    }

    @Test
    @DisplayName("Should accept valid JPEG image")
    void shouldAcceptValidJpeg() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "fake-content".getBytes());

        String result = fileUploadService.storeFile(file);

        assertThat(result).startsWith("/uploads/");
        assertThat(result).endsWith(".jpg");
    }

    @Test
    @DisplayName("Should reject file exceeding 5MB")
    void shouldRejectTooLargeFile() {
        byte[] largeContent = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", largeContent);

        assertThatThrownBy(() -> fileUploadService.storeFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    @DisplayName("Should reject executable files (MIME type check)")
    void shouldRejectExecutableFiles() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "virus.exe", "application/x-msdownload", "fake-content".getBytes());

        assertThatThrownBy(() -> fileUploadService.storeFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid file type");
    }

    @Test
    @DisplayName("Should reject files with wrong extension")
    void shouldRejectWrongExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "script.php.jpg", "image/jpeg", "fake-content".getBytes());

        String result = fileUploadService.storeFile(file);
        assertThat(result).endsWith(".jpg");
    }

    @Test
    @DisplayName("Should return null for empty file")
    void shouldReturnNullForEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThat(fileUploadService.storeFile(file)).isNull();
    }

    @Test
    @DisplayName("Should prevent path traversal attack")
    void shouldPreventPathTraversal() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "../../../etc/passwd", "image/jpeg", "fake-content".getBytes());

        assertThatThrownBy(() -> fileUploadService.storeFile(file))
                .isInstanceOf(IllegalArgumentException.class);
    }
}