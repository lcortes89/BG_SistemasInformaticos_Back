package com.bgsistemas.api.upload;

import com.bgsistemas.api.upload.exceptions.InvalidImageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

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
}
