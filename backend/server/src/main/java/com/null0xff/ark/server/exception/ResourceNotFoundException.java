package com.null0xff.ark.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found in the database.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BaseApiException {
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, UUID resourceId) {
        super(message, resourceId);
    }
}
