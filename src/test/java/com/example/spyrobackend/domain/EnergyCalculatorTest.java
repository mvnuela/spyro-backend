package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.api.ChargingWindowDto;
import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnergyCalculatorTest {

    private final EnergyCalculator calculator = new EnergyCalculator();

    private GenerationInterval interval(FuelShare... fuels) {
        return new GenerationInterval(null, null, List.of(fuels));
    }

    private GenerationInterval windInterval(String from, String to, double windPerc) {
        return new GenerationInterval(
                OffsetDateTime.parse(from),
                OffsetDateTime.parse(to),
                List.of(new FuelShare("wind", windPerc), new FuelShare("gas", 100 - windPerc))
        );
    }

    @Test
    void sumsOnlyCleanSources() {
        List<FuelShare> mix = List.of(
                new FuelShare("wind", 30.0),
                new FuelShare("nuclear", 20.0),
                new FuelShare("solar", 5.0),
                new FuelShare("gas", 40.0),
                new FuelShare("coal", 5.0)
        );

        double clean = calculator.cleanPercentage(mix);

        assertThat(clean).isEqualTo(55.0);
    }

    @Test
    void averagesEachFuelAcrossIntervals() {
        List<GenerationInterval> intervals = List.of(
                interval(new FuelShare("wind", 40.0), new FuelShare("gas", 60.0)),
                interval(new FuelShare("wind", 20.0), new FuelShare("gas", 80.0))
        );

        List<FuelShare> average = calculator.averageMix(intervals);

        assertThat(average).containsExactlyInAnyOrder(
                new FuelShare("wind", 30.0),
                new FuelShare("gas", 70.0)
        );
    }

    @Test
    void computesDailyCleanPercentageFromAveragedMix() {
        List<GenerationInterval> intervals = List.of(
                interval(new FuelShare("wind", 40.0), new FuelShare("gas", 60.0)),
                interval(new FuelShare("wind", 20.0), new FuelShare("gas", 80.0))
        );

        double dayClean = calculator.cleanPercentage(calculator.averageMix(intervals));

        assertThat(dayClean).isEqualTo(30.0);
    }

    @Test
    void roundsAveragedFuelSharesToTwoDecimals() {
        List<GenerationInterval> intervals = List.of(
                interval(new FuelShare("wind", 10.0), new FuelShare("gas", 90.0)),
                interval(new FuelShare("wind", 10.0), new FuelShare("gas", 90.0)),
                interval(new FuelShare("wind", 11.0), new FuelShare("gas", 89.0))
        );

        List<FuelShare> average = calculator.averageMix(intervals);

        // wind (10+10+11)/3 = 10.333... -> 10.33 ; gas (90+90+89)/3 = 89.666... -> 89.67
        assertThat(average).containsExactlyInAnyOrder(
                new FuelShare("wind", 10.33),
                new FuelShare("gas", 89.67)
        );
    }

    @Test
    void picksWindowWithHighestAverageCleanEnergy() {
        List<GenerationInterval> intervals = List.of(
                windInterval("2026-06-13T00:00:00Z", "2026-06-13T00:30:00Z", 10),
                windInterval("2026-06-13T00:30:00Z", "2026-06-13T01:00:00Z", 20),
                windInterval("2026-06-13T01:00:00Z", "2026-06-13T01:30:00Z", 80),
                windInterval("2026-06-13T01:30:00Z", "2026-06-13T02:00:00Z", 90)
        );

        ChargingWindowDto window = calculator.bestChargingWindow(intervals, 1);

        assertThat(window.start()).isEqualTo(OffsetDateTime.parse("2026-06-13T01:00:00Z"));
        assertThat(window.end()).isEqualTo(OffsetDateTime.parse("2026-06-13T02:00:00Z"));
        assertThat(window.averageCleanEnergyPercentage()).isEqualTo(85.0);
    }

    @Test
    void windowCanSpanTwoDays() {
        List<GenerationInterval> intervals = List.of(
                windInterval("2026-06-13T23:00:00Z", "2026-06-13T23:30:00Z", 50),
                windInterval("2026-06-13T23:30:00Z", "2026-06-14T00:00:00Z", 95),
                windInterval("2026-06-14T00:00:00Z", "2026-06-14T00:30:00Z", 96),
                windInterval("2026-06-14T00:30:00Z", "2026-06-14T01:00:00Z", 40)
        );

        ChargingWindowDto window = calculator.bestChargingWindow(intervals, 1);

        assertThat(window.start()).isEqualTo(OffsetDateTime.parse("2026-06-13T23:30:00Z"));
        assertThat(window.end()).isEqualTo(OffsetDateTime.parse("2026-06-14T00:30:00Z"));
        assertThat(window.averageCleanEnergyPercentage()).isEqualTo(95.5);
    }

    @Test
    void throwsWhenNotEnoughIntervalsForWindow() {
        List<GenerationInterval> intervals = List.of(
                windInterval("2026-06-13T00:00:00Z", "2026-06-13T00:30:00Z", 50),
                windInterval("2026-06-13T00:30:00Z", "2026-06-13T01:00:00Z", 60),
                windInterval("2026-06-13T01:00:00Z", "2026-06-13T01:30:00Z", 70)
        );

        assertThatThrownBy(() -> calculator.bestChargingWindow(intervals, 2))
                .isInstanceOf(InsufficientDataException.class);
    }

    @Test
    void skipsWindowsWithATimeGap() {
        List<GenerationInterval> intervals = List.of(
                windInterval("2026-06-13T10:00:00Z", "2026-06-13T10:30:00Z", 50),
                windInterval("2026-06-13T10:30:00Z", "2026-06-13T11:00:00Z", 60),
                windInterval("2026-06-13T11:30:00Z", "2026-06-13T12:00:00Z", 100)
        );

        ChargingWindowDto window = calculator.bestChargingWindow(intervals, 1);

        assertThat(window.averageCleanEnergyPercentage()).isEqualTo(55.0);
        assertThat(window.start()).isEqualTo(OffsetDateTime.parse("2026-06-13T10:00:00Z"));
        assertThat(window.end()).isEqualTo(OffsetDateTime.parse("2026-06-13T11:00:00Z"));
    }

    @Test
    void throwsWhenNoContiguousWindowExists() {
        List<GenerationInterval> intervals = List.of(
                windInterval("2026-06-13T10:00:00Z", "2026-06-13T10:30:00Z", 50),
                windInterval("2026-06-13T11:00:00Z", "2026-06-13T11:30:00Z", 60),
                windInterval("2026-06-13T12:00:00Z", "2026-06-13T12:30:00Z", 70)
        );

        assertThatThrownBy(() -> calculator.bestChargingWindow(intervals, 1))
                .isInstanceOf(InsufficientDataException.class);
    }
}
