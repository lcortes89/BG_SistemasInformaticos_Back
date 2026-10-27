package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.exceptions.InvalidImageException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

// The rules of the images, the same for every storage (BGSI-327)
final class ImageFiles {

    static final long MAX_SIZE_BYTES = 2 * 1024 * 1024; // 2 MB

    // Allowed types -> extension of the saved file
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    // The names we generate: only these files are ever listed or deleted
    private static final Pattern GENERATED_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private ImageFiles() {
    }

    // Checks the file and returns a new random name: two files called "logo.png" never overwrite each other
    static String newName(MultipartFile file) {
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
        return UUID.randomUUID() + "." + extension;
    }

    static boolean isGeneratedName(String fileName) {
        return fileName != null && GENERATED_NAME.matcher(fileName).matches();
    }
}
