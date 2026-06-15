package com.example.spyrobackend.service;

import com.example.spyrobackend.client.CarbonIntensityClient;
import com.example.spyrobackend.domain.EnergyCalculator;
import com.example.spyrobackend.dto.api.DailyMixDto;
import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the three-day (today, tomorrow, day after) averaged energy mix.
 */
@Service
public class EnergyMixService {

    private static final ZoneId UK = ZoneId.of("Europe/London");

    private final CarbonIntensityClient client;
    private final EnergyCalculator calculator;
    private final Clock clock;

    public EnergyMixService(CarbonIntensityClient client, EnergyCalculator calculator, Clock clock) {
        this.client = client;
        this.calculator = calculator;
        this.clock = clock;
    }

    public List<DailyMixDto> getThreeDayMix() {
        LocalDate today = LocalDate.now(clock.withZone(UK));

        // Shared, cached 3-day fetch (reused by the charging-window endpoint too).
        List<GenerationInterval> intervals = client.getThreeDayGenerationMix();

        Map<LocalDate, List<GenerationInterval>> byDate = intervals.stream()
                .collect(Collectors.groupingBy(i -> i.from().atZoneSameInstant(UK).toLocalDate()));

        List<DailyMixDto> days = new ArrayList<>();
        for (int offset = 0; offset < 3; offset++) {
            LocalDate date = today.plusDays(offset);
            List<GenerationInterval> dayIntervals = byDate.getOrDefault(date, List.of());

            if (dayIntervals.isEmpty()) {
                days.add(new DailyMixDto(date, List.of(), 0.0));
            } else {
                List<FuelShare> average = calculator.averageMix(dayIntervals);
                double clean = calculator.cleanPercentage(average);
                days.add(new DailyMixDto(date, average, clean));
            }
        }
        return days;
    }
}