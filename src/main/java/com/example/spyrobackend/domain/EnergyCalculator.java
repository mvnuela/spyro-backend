package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.external.FuelShare;

import java.util.List;
import java.util.Set;

public class EnergyCalculator {

    public static final Set<String> CLEAN_FUELS =
            Set.of("biomass", "nuclear", "hydro", "wind", "solar");

    public double cleanPercentage(List<FuelShare> generationMix) {
        return generationMix.stream()
                .filter(share -> CLEAN_FUELS.contains(share.fuel()))
                .mapToDouble(FuelShare::perc)
                .sum();
    }
}