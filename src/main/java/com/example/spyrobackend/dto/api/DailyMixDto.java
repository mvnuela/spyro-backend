package com.example.spyrobackend.dto.api;

import com.example.spyrobackend.dto.external.FuelShare;

import java.time.LocalDate;
import java.util.List;

/**
 * Response returned to the frontend: averaged energy mix for a single day.
 */
public record DailyMixDto(LocalDate date, List<FuelShare> generationMix, double cleanEnergyPercentage) {
}
