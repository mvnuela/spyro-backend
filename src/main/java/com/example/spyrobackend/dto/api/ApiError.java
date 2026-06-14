package com.example.spyrobackend.dto.api;

/**
 * Uniform error body returned to the frontend.
 */
public record ApiError(String code, String message) {
}