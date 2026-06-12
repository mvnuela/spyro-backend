package com.example.spyrobackend.dto.api;

import java.time.OffsetDateTime;

/**
 * Response returned to the frontend: the optimal charging window.
 */
public record ChargingWindowDto(OffsetDateTime start, OffsetDateTime end, double averageCleanEnergyPercentage) {
}