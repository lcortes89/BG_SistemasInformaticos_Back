package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.upload.ImageStorageService;
import com.bgsistemas.api.upload.StoredImage;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BGSI-327: the nightly cleanup of the images nobody uses
class UnusedImageCleanupTest {

    private static final Instant NOW = Instant.parse("2026-10-27T03:45:00Z");

    private final ImageStorageService storage = mock(ImageStorageService.class);
    private final ImageReferences references = mock(ImageReferences.class);
    private final UnusedImageCleanup cleanup = new UnusedImageCleanup(storage, references, Clock.fixed(NOW, ZoneOffset.UTC));

    private StoredImage image(String name, Duration age) {
        return new StoredImage(name, NOW.minus(age));
    }

    @Test
    void deletesOnlyTheOldImagesThatNothingUses() {
        when(references.fileNamesInUse()).thenReturn(Set.of("logo-in-use.png"));
        when(storage.list()).thenReturn(List.of(
                image("old-unused.png", Duration.ofDays(3)),
                image("logo-in-use.png", Duration.ofDays(30)),
                image("just-uploaded.png", Duration.ofHours(2)))); // a form may still be open

        int deleted = cleanup.deleteUnusedImages();

        assertThat(deleted).isEqualTo(1);
        verify(storage).delete("old-unused.png");
        verify(storage, never()).delete("logo-in-use.png");
        verify(storage, never()).delete("just-uploaded.png");
    }

    @Test
    void deletesNothingWhenTheDatabaseCannotBeRead() {
        when(references.fileNamesInUse()).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(cleanup::deleteUnusedImages).isInstanceOf(IllegalStateException.class);
        verify(storage, never()).list();
        verify(storage, never()).delete(any());
    }

    @Test
    void runsEveryNight() throws NoSuchMethodException {
        Scheduled scheduled = UnusedImageCleanup.class.getMethod("deleteUnusedImages").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${app.cleanup.images-cron:0 45 3 * * *}");
    }
}
