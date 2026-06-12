package com.example.spyrobackend.dto.external;

import java.util.List;

/**
 * Top-level shape of the Carbon Intensity /generation response:
 * { "data": [ { from, to, generationmix:[...] }, ... ] }.
 */
public record CarbonIntensityResponse(List<GenerationInterval> data) {
}