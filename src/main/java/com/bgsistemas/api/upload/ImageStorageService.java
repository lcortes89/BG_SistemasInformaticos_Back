package com.bgsistemas.api.upload;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {

    // Validates and saves the image; returns the generated file name (e.g. "3f2a....png")
    String store(MultipartFile file);
}
