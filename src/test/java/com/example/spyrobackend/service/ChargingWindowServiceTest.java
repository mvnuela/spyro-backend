package com.example.spyrobackend.service;

import com.example.spyrobackend.client.CarbonIntensityClient;
import com.example.spyrobackend.domain.EnergyCalculator;
import com.example.spyrobackend.dto.api.ChargingWindowDto;
import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChargingWindowServiceTest {

    private final CarbonIntensityClient client = mock(CarbonIntensityClient.class);
    private final EnergyCalculator calculator = new EnergyCalculator();
    // "Now" frozen at 2026-06-13T10:00Z -> window of interest is [now, now+48h).
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-13T10:00:00Z"), ZoneOffset.UTC);

    private final ChargingWindowService service = new ChargingWindowService(client, calculator, clock);

    private GenerationInterval windInterval(String fromUtc, double windPerc) {
        OffsetDateTime from = OffsetDateTime.parse(fromUtc);
        return new GenerationInterval(from, from.plusMinutes(30),
                List.of(new FuelShare("wind", windPerc), new FuelShare("gas", 100 - windPerc)));
    }

    @Test
    void returnsTheCleanestWindowFromTheFetchedData() {
        when(client.getThreeDayGenerationMix()).thenReturn(List.of(
                windInterval("2026-06-13T11:00:00Z", 10),
                windInterval("2026-06-13T11:30:00Z", 20),
                windInterval("2026-06-13T12:00:00Z", 80),
                windInterval("2026-06-13T12:30:00Z", 90)
        ));

        // hours=1 -> 2 intervals; cleanest is the last two: (80+90)/2 = 85
        ChargingWindowDto window = service.findOptimalWindow(1);

        assertThat(window.start()).isEqualTo(OffsetDateTime.parse("2026-06-13T12:00:00Z"));
        assertThat(window.end()).isEqualTo(OffsetDateTime.parse("2026-06-13T13:00:00Z"));
        assertThat(window.averageCleanEnergyPercentage()).isEqualTo(85.0);
    }

    @Test
    void ignoresIntervalsThatAlreadyStartedBeforeNow() {
        // now = 2026-06-13T10:00Z. The 09:30 interval is the cleanest, but it has already
        // started -> we must NOT propose a window that begins in the past.
        when(client.getThreeDayGenerationMix()).thenReturn(List.of(
                windInterval("2026-06-13T09:30:00Z", 100), // already running -> excluded
                windInterval("2026-06-13T10:00:00Z", 95),
                windInterval("2026-06-13T10:30:00Z", 10),
                windInterval("2026-06-13T11:00:00Z", 20),
                windInterval("2026-06-13T11:30:00Z", 30)
        ));

        ChargingWindowDto window = service.findOptimalWindow(1);

        // If 09:30 were used, the best window (09:30+10:00)=97.5 would start in the past.
        // It is excluded, so the best in-range window is (10:00+10:30)/2 = 52.5, starting at 10:00.
        assertThat(window.start()).isEqualTo(OffsetDateTime.parse("2026-06-13T10:00:00Z"));
        assertThat(window.averageCleanEnergyPercentage()).isEqualTo(52.5);
    }

    @Test
    void reusesTheSharedFetchOnceAndIgnoresDataBeyondTheNextTwoDays() {
        when(client.getThreeDayGenerationMix()).thenReturn(List.of(
                windInterval("2026-06-13T11:00:00Z", 10),
                windInterval("2026-06-13T11:30:00Z", 20),
                windInterval("2026-06-13T12:00:00Z", 80),
                windInterval("2026-06-13T12:30:00Z", 90),
                // beyond now+48h (now = 2026-06-13T10:00Z) -> must be ignored
                windInterval("2026-06-15T11:00:00Z", 100)
        ));

        ChargingWindowDto window = service.findOptimalWindow(1);

        // If the out-of-range interval were used, the window (90+100)/2=95 would win.
        // It is filtered out, so the best stays (80+90)/2 = 85, ending at 13:00.
        assertThat(window.averageCleanEnergyPercentage()).isEqualTo(85.0);
        assertThat(window.end()).isEqualTo(OffsetDateTime.parse("2026-06-13T13:00:00Z"));
        verify(client, times(1)).getThreeDayGenerationMix();
    }
}