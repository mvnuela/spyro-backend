package com.example.spyrobackend.service;

import com.example.spyrobackend.client.CarbonIntensityClient;
import com.example.spyrobackend.domain.EnergyCalculator;
import com.example.spyrobackend.dto.api.ChargingWindowDto;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Finds the optimal charging window over the next two days.
 */
@Service
public class ChargingWindowService {

    private final CarbonIntensityClient client;
    private final EnergyCalculator calculator;
    private final Clock clock;

    public ChargingWindowService(CarbonIntensityClient client, EnergyCalculator calculator, Clock clock) {
        this.client = client;
        this.calculator = calculator;
        this.clock = clock;
    }

    public ChargingWindowDto findOptimalWindow(int hours) {
        // Reuse the shared, cached 3-day fetch, then keep only the next two days from now.
        Instant from = Instant.now(clock);
        Instant to = from.plus(Duration.ofDays(2));

        List<GenerationInterval> nextTwoDays = client.getThreeDayGenerationMix().stream()
                .filter(interval -> {
                    Instant start = interval.from().toInstant();
                    return !start.isBefore(from) && start.isBefore(to);
                })
                .collect(Collectors.toList());

        return calculator.bestChargingWindow(nextTwoDays, hours);
    }
}