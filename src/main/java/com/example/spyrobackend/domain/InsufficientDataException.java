package com.example.spyrobackend.domain;

/**
 * Thrown when the external API did not return enough intervals
 * to build a charging window of the requested length.
 */
public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }
}