package com.null0xff.ark.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

/**
 * Exception thrown when a user lacks permission to access a resource or perform an action.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends BaseApiException {
    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(String message, UUID resourceId) {
        super(message, resourceId);
    }
}
