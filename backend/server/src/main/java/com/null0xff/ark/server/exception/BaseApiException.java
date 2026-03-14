package com.null0xff.ark.server.exception;

import lombok.Getter;
import java.util.UUID;

/**
 * Base class for custom API exceptions that can carry an optional resource UUID.
 */
@Getter
public abstract class BaseApiException extends RuntimeException {
    private final UUID resourceId;

    public BaseApiException(String message) {
        super(message);
        this.resourceId = null;
    }

    public BaseApiException(String message, UUID resourceId) {
        super(message);
        this.resourceId = resourceId;
    }
}
