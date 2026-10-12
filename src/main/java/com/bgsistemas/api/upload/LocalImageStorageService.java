package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.exceptions.InvalidImageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

// Saves the images in a folder of the server (app.uploads.dir)
@Component
public class LocalImageStorageService implements ImageStorageService {

    static final long MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB

    // Allowed types -> extension of the saved file
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path uploadDir;

    public LocalImageStorageService(@Value("${app.uploads.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("The file is empty");
        }
        String extension = EXTENSIONS.get(file.getContentType());
        if (extension == null) {
            throw new InvalidImageException("Only JPG, PNG or WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidImageException("The image must be at most 2 MB");
        }

        // Random name: two files called "logo.png" never overwrite each other
        String fileName = UUID.randomUUID() + "." + extension;
        try (InputStream content = file.getInputStream()) {
            Files.createDirectories(uploadDir);
            Files.copy(content, uploadDir.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the image", e);
        }
        return fileName;
    }
}
