package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.exceptions.ImageStorageException;
import com.bgsistemas.api.upload.exceptions.InvalidImageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// BGSI-327: the calls to the Storage API of Supabase, against a fake server
class SupabaseImageStorageServiceTest {

    private static final String URL = "https://abc.supabase.co";
    private static final String SECRET_KEY = "sb_secret_test";
    private static final String NAME = "3f2a1b4c-0d9e-4f8a-b7c6-1234567890ab.png";

    private final MockMultipartFile png = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

    private RestClient.Builder builder;
    private MockRestServiceServer server;

    @BeforeEach
    void bindFakeServer() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private SupabaseImageStorageService storage(String key) {
        return new SupabaseImageStorageService(builder, URL + "/", key, "images");
    }

    @Test
    void uploadsTheImageToTheBucketAndGivesItsPublicUrl() {
        server.expect(requestTo(startsWith(URL + "/storage/v1/object/images/")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("apikey", SECRET_KEY))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION)) // a secret key is not a JWT
                .andExpect(header(HttpHeaders.CACHE_CONTROL, "max-age=31536000"))
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andRespond(withSuccess("{\"Key\":\"images/x.png\"}", MediaType.APPLICATION_JSON));
        SupabaseImageStorageService storage = storage(SECRET_KEY);

        String fileName = storage.store(png);

        server.verify();
        assertThat(fileName).endsWith(".png").isNotEqualTo("logo.png");
        assertThat(storage.publicUrl(fileName)).isEqualTo(URL + "/storage/v1/object/public/images/" + fileName);
    }

    @Test
    void theOldServiceRoleKeyAlsoGoesAsBearer() {
        server.expect(requestTo(startsWith(URL + "/storage/v1/object/images/")))
                .andExpect(header("apikey", "eyJold"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer eyJold"))
                .andRespond(withSuccess());

        storage("eyJold").store(png);

        server.verify();
    }

    @Test
    void checksTheImageBeforeCallingSupabase() {
        MockMultipartFile text = new MockMultipartFile("file", "notes.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> storage(SECRET_KEY).store(text)).isInstanceOf(InvalidImageException.class);
        server.verify(); // no call
    }

    @Test
    void saysWhenSupabaseFails() {
        server.expect(requestTo(startsWith(URL + "/storage/v1/object/images/"))).andRespond(withServerError());

        assertThatThrownBy(() -> storage(SECRET_KEY).store(png))
                .isInstanceOf(ImageStorageException.class)
                .hasMessage("Could not save the image in Supabase");
    }

    @Test
    void listsOnlyOurImagesWithTheirDate() {
        server.expect(requestTo(URL + "/storage/v1/object/list/images"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.prefix").value(""))
                .andExpect(jsonPath("$.offset").value(0))
                .andRespond(withSuccess("""
                        [
                          {"name": "old", "id": null},
                          {"name": "%s", "id": "1", "updated_at": "2026-10-20T10:00:00.000Z"},
                          {"name": "logo-by-hand.png", "id": "2", "updated_at": "2026-10-20T10:00:00.000Z"}
                        ]""".formatted(NAME), MediaType.APPLICATION_JSON));

        var images = storage(SECRET_KEY).list();

        assertThat(images).containsExactly(new StoredImage(NAME, Instant.parse("2026-10-20T10:00:00Z")));
    }

    @Test
    void deletesOneImage() {
        server.expect(requestTo(URL + "/storage/v1/object/images/" + NAME))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        storage(SECRET_KEY).delete(NAME);

        server.verify();
    }

    @Test
    void neverDeletesNamesItDidNotGenerate() {
        storage(SECRET_KEY).delete("../other-bucket/file.png");

        server.verify(); // no call
    }

    @Test
    void needsTheUrlAndTheKey() {
        assertThatThrownBy(() -> new SupabaseImageStorageService(builder, "", SECRET_KEY, "images"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new SupabaseImageStorageService(builder, URL, " ", "images"))
                .isInstanceOf(IllegalStateException.class);
    }
}
