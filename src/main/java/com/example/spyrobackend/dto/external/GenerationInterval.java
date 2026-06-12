package com.example.spyrobackend.dto.external;

import java.time.Instant;
import java.util.List;

public record GenerationInterval(Instant from, Instant to, List<FuelShare> generationmix) {
}