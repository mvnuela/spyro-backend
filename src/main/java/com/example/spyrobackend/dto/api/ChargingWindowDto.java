package com.example.spyrobackend.dto.api;

import java.time.Instant;

/**
 * Response returned to the frontend: the optimal charging window.
 */
public record ChargingWindowDto(Instant start, Instant end, double averageCleanEnergyPercentage) {
}