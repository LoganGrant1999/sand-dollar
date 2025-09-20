package com.sanddollar.budgeting;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

@Component
public class Stats {

    public double winsorizedMean(List<Double> values, double lowerPercentile, double upperPercentile) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int n = sorted.size();
        int lowerIndex = Math.max(0, (int) Math.floor(n * lowerPercentile / 100.0));
        int upperIndex = Math.min(n - 1, (int) Math.ceil(n * upperPercentile / 100.0));

        double lowerBound = sorted.get(lowerIndex);
        double upperBound = sorted.get(upperIndex);

        double sum = 0.0;
        int count = 0;

        for (double value : values) {
            double winsorizedValue = Math.max(lowerBound, Math.min(upperBound, value));
            sum += winsorizedValue;
            count++;
        }

        return count > 0 ? sum / count : 0.0;
    }

    public double exponentialMovingAverage(List<Double> values, double alpha) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        double ema = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            ema = alpha * values.get(i) + (1 - alpha) * ema;
        }

        return ema;
    }

    public double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);

        int n = sorted.size();
        if (n % 2 == 0) {
            return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
        } else {
            return sorted.get(n / 2);
        }
    }

    public double mean(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        return sum / values.size();
    }

    public double standardDeviation(List<Double> values) {
        if (values == null || values.size() < 2) {
            return 0.0;
        }

        double mean = mean(values);
        double variance = values.stream()
            .mapToDouble(value -> Math.pow(value - mean, 2))
            .sum() / (values.size() - 1);

        return Math.sqrt(variance);
    }

    public boolean isWithinTolerance(double value, double target, double tolerancePercent) {
        if (target == 0.0) {
            return Math.abs(value) <= tolerancePercent / 100.0;
        }

        double tolerance = Math.abs(target * tolerancePercent / 100.0);
        return Math.abs(value - target) <= tolerance;
    }
}