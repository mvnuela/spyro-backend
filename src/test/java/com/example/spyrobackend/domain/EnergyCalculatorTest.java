package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyCalculatorTest {

    private final EnergyCalculator calculator = new EnergyCalculator();

    private GenerationInterval interval(FuelShare... fuels) {
        return new GenerationInterval(null, null, List.of(fuels));
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
}
