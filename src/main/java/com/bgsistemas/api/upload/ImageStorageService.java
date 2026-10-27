package com.bgsistemas.api.upload;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Where the uploaded images live. Two implementations, chosen with app.storage.type (BGSI-327):
// "local" (default) -> a folder of the server; "supabase" -> a public bucket of Supabase Storage
public interface ImageStorageService {

    // Validates and saves the image; returns the generated file name (e.g. "3f2a....png")
    String store(MultipartFile file);

    // BGSI-327: the URL saved in the database and shown on the website
    String publicUrl(String fileName);

    // BGSI-327: the saved images, for the nightly cleanup of the ones nobody uses
    List<StoredImage> list();

    void delete(String fileName);
}
