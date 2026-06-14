package com.example.spyrobackend.service;

import com.example.spyrobackend.client.CarbonIntensityClient;
import com.example.spyrobackend.domain.EnergyCalculator;
import com.example.spyrobackend.dto.api.DailyMixDto;
import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnergyMixServiceTest {

    private final CarbonIntensityClient client = mock(CarbonIntensityClient.class);
    private final EnergyCalculator calculator = new EnergyCalculator();
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneOffset.UTC);

    private final EnergyMixService service = new EnergyMixService(client, calculator, clock);

    private GenerationInterval windInterval(String fromUtc, double windPerc) {
        OffsetDateTime from = OffsetDateTime.parse(fromUtc);
        return new GenerationInterval(from, from.plusMinutes(30),
                List.of(new FuelShare("wind", windPerc), new FuelShare("gas", 100 - windPerc)));
    }

    @Test
    void groupsIntervalsByUkDateAndAveragesEachOfThreeDays() {
        when(client.getThreeDayGenerationMix()).thenReturn(List.of(
                windInterval("2026-06-13T11:00:00Z", 40),
                windInterval("2026-06-13T11:30:00Z", 20),
                windInterval("2026-06-14T11:00:00Z", 50),
                windInterval("2026-06-15T11:00:00Z", 10)
        ));

        List<DailyMixDto> result = service.getThreeDayMix();

        assertThat(result).extracting(DailyMixDto::date).containsExactly(
                LocalDate.of(2026, 6, 13),
                LocalDate.of(2026, 6, 14),
                LocalDate.of(2026, 6, 15));
        assertThat(result.get(0).cleanEnergyPercentage()).isEqualTo(30.0);
        assertThat(result.get(0).generationMix()).containsExactlyInAnyOrder(
                new FuelShare("wind", 30.0), new FuelShare("gas", 70.0));
        assertThat(result.get(0).intervalCount()).isEqualTo(2); // two intervals on the 13th
        assertThat(result.get(1).cleanEnergyPercentage()).isEqualTo(50.0);
        assertThat(result.get(1).intervalCount()).isEqualTo(1);
        assertThat(result.get(2).cleanEnergyPercentage()).isEqualTo(10.0);
        assertThat(result.get(2).intervalCount()).isEqualTo(1);
    }

    @Test
    void returnsZeroCleanForDaysWithoutData() {
        when(client.getThreeDayGenerationMix()).thenReturn(List.of());

        List<DailyMixDto> result = service.getThreeDayMix();

        assertThat(result).hasSize(3);
        assertThat(result).allSatisfy(day -> {
            assertThat(day.generationMix()).isEmpty();
            assertThat(day.cleanEnergyPercentage()).isEqualTo(0.0);
            assertThat(day.intervalCount()).isEqualTo(0);
        });
    }
}