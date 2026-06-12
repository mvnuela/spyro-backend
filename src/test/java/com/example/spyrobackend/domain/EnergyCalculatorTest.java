package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.external.FuelShare;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EnergyCalculatorTest {

    private final EnergyCalculator calculator = new EnergyCalculator();

    @Test
    void cleanPercentage_sumujeTylkoCzysteZrodla() {

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
}