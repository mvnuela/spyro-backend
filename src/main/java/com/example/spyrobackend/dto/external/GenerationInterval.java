package com.example.spyrobackend.dto.external;

import java.time.OffsetDateTime;
import java.util.List;

public record GenerationInterval(OffsetDateTime from, OffsetDateTime to, List<FuelShare> generationmix) {
}