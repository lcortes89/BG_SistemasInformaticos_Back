package com.bgsistemas.api.upload;

import java.time.Instant;

// BGSI-327: one saved image and when it was saved
public record StoredImage(String name, Instant savedAt) {
}
