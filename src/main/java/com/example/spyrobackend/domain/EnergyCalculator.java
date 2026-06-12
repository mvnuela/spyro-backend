package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    /**
     * Usrednia udzial kazdego paliwa po wszystkich interwalach.
     * Np. wind {40, 20} -> wind 30.
     */
    public List<FuelShare> averageMix(List<GenerationInterval> intervals) {
        // Sumujemy perc per paliwo, zachowujac kolejnosc pierwszego wystapienia.
        Map<String, Double> sumPerFuel = new LinkedHashMap<>();
        for (GenerationInterval interval : intervals) {
            for (FuelShare share : interval.generationmix()) {
                sumPerFuel.merge(share.fuel(), share.perc(), Double::sum);
            }
        }

        int count = intervals.size();
        return sumPerFuel.entrySet().stream()
                .map(entry -> new FuelShare(entry.getKey(), entry.getValue() / count))
                .toList();
    }
}