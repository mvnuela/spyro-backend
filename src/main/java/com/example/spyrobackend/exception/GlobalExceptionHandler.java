package com.example.spyrobackend.exception;

import com.example.spyrobackend.client.UpstreamUnavailableException;
import com.example.spyrobackend.domain.InsufficientDataException;
import com.example.spyrobackend.dto.api.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(ConstraintViolationException ex) {
        return new ApiError("VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(InsufficientDataException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public ApiError handleInsufficientData(InsufficientDataException ex) {
        return new ApiError("INSUFFICIENT_DATA", ex.getMessage());
    }

    @ExceptionHandler({WebClientException.class, UpstreamUnavailableException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError handleUpstreamFailure() {
        return new ApiError("UPSTREAM_ERROR", "Carbon Intensity API is currently unavailable");
    }
}