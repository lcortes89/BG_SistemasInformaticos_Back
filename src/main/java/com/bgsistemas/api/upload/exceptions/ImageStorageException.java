package com.bgsistemas.api.upload.exceptions;

// BGSI-327: the storage (e.g. Supabase) did not answer or refused the request
public class ImageStorageException extends RuntimeException {

    public ImageStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
