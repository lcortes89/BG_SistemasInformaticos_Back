package com.bgsistemas.api.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

// Saves the images in a folder of the server (app.uploads.dir). Default storage (app.storage.type=local)
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalImageStorageService implements ImageStorageService {

    static final long MAX_SIZE_BYTES = ImageFiles.MAX_SIZE_BYTES;

    private final Path uploadDir;

    public LocalImageStorageService(@Value("${app.uploads.dir:uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file) {
        String fileName = ImageFiles.newName(file);
        try (InputStream content = file.getInputStream()) {
            Files.createDirectories(uploadDir);
            Files.copy(content, uploadDir.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the image", e);
        }
        return fileName;
    }

    // e.g. http://localhost:8080/uploads/3f2a....png (UploadResourceConfig serves the folder)
    @Override
    public String publicUrl(String fileName) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(fileName)
                .toUriString();
    }

    @Override
    public List<StoredImage> list() {
        if (!Files.isDirectory(uploadDir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(uploadDir)) {
            return files
                    .filter(path -> ImageFiles.isGeneratedName(path.getFileName().toString()))
                    .map(path -> new StoredImage(path.getFileName().toString(), lastModified(path)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the images folder", e);
        }
    }

    // Only files with the names we generate, and only inside the folder
    @Override
    public void delete(String fileName) {
        if (!ImageFiles.isGeneratedName(fileName)) {
            return;
        }
        try {
            Files.deleteIfExists(uploadDir.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete the image", e);
        }
    }

    private static Instant lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toInstant();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the image date", e);
        }
    }
}
