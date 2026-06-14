package com.example.spyrobackend.client;

/**
 * Thrown when the external Carbon Intensity API is unreachable or too slow (timeout).
 * Mapped to HTTP 503 by the global exception handler.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}