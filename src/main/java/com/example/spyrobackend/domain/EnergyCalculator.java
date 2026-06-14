package com.example.spyrobackend.domain;

import com.example.spyrobackend.dto.api.ChargingWindowDto;
import com.example.spyrobackend.dto.external.FuelShare;
import com.example.spyrobackend.dto.external.GenerationInterval;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class EnergyCalculator {

    public static final Set<String> CLEAN_FUELS =
            Set.of("biomass", "nuclear", "hydro", "wind", "solar");

    public double cleanPercentage(List<FuelShare> generationMix) {
        double sum = generationMix.stream()
                .filter(share -> CLEAN_FUELS.contains(share.fuel()))
                .mapToDouble(FuelShare::perc)
                .sum();
        return round2(sum); // avoid binary float noise (e.g. 68.14000000000001)
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
                .map(entry -> new FuelShare(entry.getKey(), round2(entry.getValue() / count)))
                .toList();
    }


    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public ChargingWindowDto bestChargingWindow(List<GenerationInterval> intervals, int hours) {
        int windowSize = 2 * hours;
        if (intervals.size() < windowSize) {
            throw new InsufficientDataException(
                    "Need at least " + windowSize + " intervals for a " + hours + "h window, got " + intervals.size());
        }

        int bestStart = -1;
        double bestAverage = -1;
        for (int start = 0; start + windowSize <= intervals.size(); start++) {
            if (!isContiguous(intervals, start, windowSize)) {
                continue; // skip windows that have a time gap - not a real continuous slot
            }
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

        if (bestStart < 0) {
            throw new InsufficientDataException(
                    "No contiguous " + hours + "h window available in the provided data");
        }

        GenerationInterval first = intervals.get(bestStart);
        GenerationInterval last = intervals.get(bestStart + windowSize - 1);
        return new ChargingWindowDto(first.from(), last.to(), round2(bestAverage));
    }

    /** True if the {@code windowSize} intervals from {@code start} are back-to-back in time. */
    private static boolean isContiguous(List<GenerationInterval> intervals, int start, int windowSize) {
        for (int i = start; i < start + windowSize - 1; i++) {
            // isEqual compares the instant on the timeline (robust to Z vs +00:00 representation).
            if (!intervals.get(i).to().isEqual(intervals.get(i + 1).from())) {
                return false;
            }
        }
        return true;
    }
}