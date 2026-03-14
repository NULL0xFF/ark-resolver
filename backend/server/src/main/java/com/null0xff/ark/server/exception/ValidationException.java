package com.null0xff.ark.server.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import java.util.UUID;

/**
 * Exception thrown when input data or request state is invalid.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ValidationException extends BaseApiException {
    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, UUID resourceId) {
        super(message, resourceId);
    }
}
