package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.dtos.UploadResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/uploads")
public class AdminUploadController {

    private final ImageStorageService imageStorageService;

    public AdminUploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    // POST /api/admin/uploads (multipart/form-data, field "file")
    // -> 201 { "url": "http://localhost:8080/uploads/3f2a....png" } (or the Supabase URL, BGSI-327)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponseDTO> uploadImage(@RequestParam("file") MultipartFile file) {
        String fileName = imageStorageService.store(file);
        // BGSI-327: local -> http://localhost:8080/uploads/x.png; Supabase -> its public URL
        String url = imageStorageService.publicUrl(fileName);
        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadResponseDTO(url));
    }
}
