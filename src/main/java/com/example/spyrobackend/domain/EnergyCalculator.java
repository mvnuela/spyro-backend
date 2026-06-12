package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.api.ChargingWindowDto;
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

    public List<FuelShare> averageMix(List<GenerationInterval> intervals) {
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

    public ChargingWindowDto bestChargingWindow(List<GenerationInterval> intervals, int hours) {
        int windowSize = 2 * hours;
        if (intervals.size() < windowSize) {
            throw new InsufficientDataException(
                    "Need at least " + windowSize + " intervals for a " + hours + "h window, got " + intervals.size());
        }

        int bestStart = 0;
        double bestAverage = -1;
        for (int start = 0; start + windowSize <= intervals.size(); start++) {
            double sum = 0;
            for (int i = start; i < start + windowSize; i++) {
                sum += cleanPercentage(intervals.get(i).generationmix());
            }
            double average = sum / windowSize;
            if (average > bestAverage) {
                bestAverage = average;
                bestStart = start;
            }
        }

        GenerationInterval first = intervals.get(bestStart);
        GenerationInterval last = intervals.get(bestStart + windowSize - 1);
        return new ChargingWindowDto(first.from(), last.to(), bestAverage);
    }
}