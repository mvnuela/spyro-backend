package com.example.spyrobackend.dto.api;

import com.example.spyrobackend.dto.external.FuelShare;

import java.time.LocalDate;
import java.util.List;

/**
 * Response returned to the frontend: averaged energy mix for a single day.
 * intervalCount is the number of half-hour intervals the average is based on
 * (a full UK day has 48); fewer than 48 means the day is only partially covered.
 */
public record DailyMixDto(LocalDate date, List<FuelShare> generationMix, double cleanEnergyPercentage,
                          int intervalCount) {
}
