package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.exceptions.ImageStorageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// BGSI-327: saves the images in a public bucket of Supabase Storage (app.storage.type=supabase).
// The images leave the server, Supabase serves them from its CDN and the URL does not depend on our domain.
// The key is the secret key of the project (sb_secret_..., or the old service_role JWT): only in .env
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "supabase")
public class SupabaseImageStorageService implements ImageStorageService {

    // The names are random, so a saved image never changes: the browsers can keep it for a year
    static final String CACHE_ONE_YEAR = "max-age=31536000";
    static final int PAGE_SIZE = 1000;

    private static final ParameterizedTypeReference<List<Map<String, Object>>> OBJECT_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient client;
    private final String bucket;
    private final String publicBaseUrl;

    @Autowired
    public SupabaseImageStorageService(@Value("${app.storage.supabase.url}") String url,
                                       @Value("${app.storage.supabase.key}") String key,
                                       @Value("${app.storage.supabase.bucket:images}") String bucket) {
        this(RestClient.builder(), url, key, bucket);
    }

    // The tests pass a builder bound to a fake server
    SupabaseImageStorageService(RestClient.Builder builder, String url, String key, String bucket) {
        // Fail at start-up, not on the first upload
        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            throw new IllegalStateException("STORAGE_TYPE=supabase needs SUPABASE_URL and SUPABASE_SERVICE_KEY");
        }
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        builder.baseUrl(base + "/storage/v1").defaultHeader("apikey", key);
        // The new secret keys (sb_secret_...) go only in "apikey"; the old service_role key is a JWT
        // and also goes as Bearer
        if (key.startsWith("eyJ")) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key);
        }
        this.client = builder.build();
        this.bucket = bucket;
        this.publicBaseUrl = base + "/storage/v1/object/public/" + bucket + "/";
    }

    // POST /storage/v1/object/{bucket}/{name} with the bytes of the image
    @Override
    public String store(MultipartFile file) {
        String fileName = ImageFiles.newName(file);
        try {
            client.post()
                    .uri("/object/{bucket}/{name}", bucket, fileName)
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CACHE_CONTROL, CACHE_ONE_YEAR)
                    .header("x-upsert", "false")
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the image", e);
        } catch (RestClientException e) {
            throw new ImageStorageException("Could not save the image in Supabase", e);
        }
        return fileName;
    }

    // e.g. https://xyz.supabase.co/storage/v1/object/public/images/3f2a....png
    @Override
    public String publicUrl(String fileName) {
        return publicBaseUrl + fileName;
    }

    // POST /storage/v1/object/list/{bucket}, 1000 by 1000
    @Override
    public List<StoredImage> list() {
        List<StoredImage> images = new ArrayList<>();
        int offset = 0;
        List<Map<String, Object>> page;
        do {
            page = listPage(offset);
            for (Map<String, Object> object : page) {
                Object name = object.get("name");
                Object savedAt = object.getOrDefault("updated_at", object.get("created_at"));
                // Folders have no id: we only want our own files
                if (object.get("id") != null && name instanceof String fileName
                        && ImageFiles.isGeneratedName(fileName) && savedAt instanceof String date) {
                    images.add(new StoredImage(fileName, parseDate(date)));
                }
            }
            offset += PAGE_SIZE;
        } while (page.size() == PAGE_SIZE);
        return images;
    }

    // DELETE /storage/v1/object/{bucket}/{name}. Only names we generate
    @Override
    public void delete(String fileName) {
        if (!ImageFiles.isGeneratedName(fileName)) {
            return;
        }
        try {
            client.delete()
                    .uri("/object/{bucket}/{name}", bucket, fileName)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ImageStorageException("Could not delete the image in Supabase", e);
        }
    }

    private List<Map<String, Object>> listPage(int offset) {
        try {
            List<Map<String, Object>> page = client.post()
                    .uri("/object/list/{bucket}", bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefix", "", "limit", PAGE_SIZE, "offset", offset,
                            "sortBy", Map.of("column", "name", "order", "asc")))
                    .retrieve()
                    .body(OBJECT_LIST);
            return page == null ? List.of() : page;
        } catch (RestClientException e) {
            throw new ImageStorageException("Could not list the images in Supabase", e);
        }
    }

    // Supabase answers "2026-10-27T10:00:00.123Z" or with an offset ("+00:00")
    private static Instant parseDate(String date) {
        return date.endsWith("Z") ? Instant.parse(date) : OffsetDateTime.parse(date).toInstant();
    }
}
