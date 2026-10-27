package com.bgsistemas.api.maintenance;

import com.bgsistemas.api.upload.ImageStorageService;
import com.bgsistemas.api.upload.StoredImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

// BGSI-327: every night deletes the uploaded images that nothing uses any more:
// the old logo after choosing a new one, the photo of a deleted product, or an image uploaded
// in a form that was then cancelled. Images younger than a day are kept (a form may still be open)
@Component
public class UnusedImageCleanup {

    private static final Logger log = LoggerFactory.getLogger(UnusedImageCleanup.class);

    static final Duration MIN_AGE = Duration.ofDays(1);

    private final ImageStorageService storage;
    private final ImageReferences references;
    private final Clock clock;

    @Autowired
    public UnusedImageCleanup(ImageStorageService storage, ImageReferences references) {
        this(storage, references, Clock.systemUTC());
    }

    // The tests choose the time
    UnusedImageCleanup(ImageStorageService storage, ImageReferences references, Clock clock) {
        this.storage = storage;
        this.references = references;
        this.clock = clock;
    }

    // Every day at 03:45 (app.cleanup.images-cron changes it; "-" turns it off). Returns how many were deleted
    @Scheduled(cron = "${app.cleanup.images-cron:0 45 3 * * *}")
    public int deleteUnusedImages() {
        // First what is in use: if the database cannot be read, nothing is deleted
        Set<String> inUse = references.fileNamesInUse();
        Instant savedBefore = Instant.now(clock).minus(MIN_AGE);
        int deleted = 0;
        for (StoredImage image : storage.list()) {
            if (image.savedAt().isBefore(savedBefore) && !inUse.contains(image.name())) {
                storage.delete(image.name());
                deleted++;
            }
        }
        if (deleted > 0) {
            log.info("Deleted {} unused images", deleted);
        }
        return deleted;
    }
}
