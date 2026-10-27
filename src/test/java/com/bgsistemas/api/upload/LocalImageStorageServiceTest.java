package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.exceptions.InvalidImageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalImageStorageServiceTest {

    // JUnit creates an empty temporary folder for each test and deletes it afterwards
    @TempDir
    Path tempDir;

    private LocalImageStorageService storage() {
        return new LocalImageStorageService(tempDir.toString());
    }

    @Test
    void shouldSavePngWithRandomName() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});

        String fileName = storage().store(file);

        assertThat(fileName).endsWith(".png").isNotEqualTo("logo.png");
        assertThat(Files.exists(tempDir.resolve(fileName))).isTrue();
    }

    @Test
    void shouldAcceptJpgAndWebp() {
        LocalImageStorageService storage = storage();

        assertThat(storage.store(new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[]{1}))).endsWith(".jpg");
        assertThat(storage.store(new MockMultipartFile("file", "b.webp", "image/webp", new byte[]{1}))).endsWith(".webp");
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> storage().store(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The file is empty");
    }

    @Test
    void shouldRejectFilesThatAreNotImages() {
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1});

        assertThatThrownBy(() -> storage().store(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("Only JPG, PNG or WebP images are allowed");
    }

    @Test
    void shouldRejectImagesBiggerThan2Mb() {
        byte[] tooBig = new byte[(int) LocalImageStorageService.MAX_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", tooBig);

        assertThatThrownBy(() -> storage().store(file))
                .isInstanceOf(InvalidImageException.class)
                .hasMessage("The image must be at most 2 MB");
    }

    // BGSI-327: URL, list and delete (for the nightly cleanup)

    @Test
    void shouldGiveTheUrlServedByTheApi() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
        try {
            assertThat(storage().publicUrl("abc.png")).isEqualTo("http://localhost/uploads/abc.png");
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldListOnlyTheImagesItSaved() throws Exception {
        LocalImageStorageService storage = storage();
        String saved = storage.store(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}));
        Files.writeString(tempDir.resolve("notes.txt"), "not an image of ours");

        assertThat(storage.list()).extracting(StoredImage::name).containsExactly(saved);
        assertThat(storage.list().get(0).savedAt()).isNotNull();
    }

    @Test
    void shouldListNothingWhenTheFolderDoesNotExistYet() {
        assertThat(new LocalImageStorageService(tempDir.resolve("missing").toString()).list()).isEmpty();
    }

    @Test
    void shouldDeleteAnImage() {
        LocalImageStorageService storage = storage();
        String saved = storage.store(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1}));

        storage.delete(saved);

        assertThat(Files.exists(tempDir.resolve(saved))).isFalse();
    }

    @Test
    void shouldNeverDeleteFilesItDidNotGenerate() throws Exception {
        Path other = Files.writeString(tempDir.resolve("keep.png"), "x");

        storage().delete("keep.png");
        storage().delete("../keep.png");

        assertThat(Files.exists(other)).isTrue();
    }
}
