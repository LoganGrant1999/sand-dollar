package com.sanddollar.budgeting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatsTest {

    private Stats stats;
    private static final double DELTA = 0.001;

    @BeforeEach
    void setUp() {
        stats = new Stats();
    }

    @Test
    void testWinsorizedMeanWithNormalData() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0);
        double result = stats.winsorizedMean(values, 10, 90);
        assertEquals(5.5, result, DELTA);
    }

    @Test
    void testWinsorizedMeanWithOutliers() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 100.0);
        double result = stats.winsorizedMean(values, 10, 90);
        // Should clip the 100.0 to 9.0 (90th percentile)
        assertTrue(result < 10.0);
    }

    @Test
    void testWinsorizedMeanWithEmptyList() {
        List<Double> values = Collections.emptyList();
        double result = stats.winsorizedMean(values, 10, 90);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testWinsorizedMeanWithSingleValue() {
        List<Double> values = Collections.singletonList(42.0);
        double result = stats.winsorizedMean(values, 10, 90);
        assertEquals(42.0, result, DELTA);
    }

    @Test
    void testExponentialMovingAverage() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        double result = stats.exponentialMovingAverage(values, 0.3);
        assertTrue(result > 1.0 && result < 5.0);
    }

    @Test
    void testExponentialMovingAverageWithEmptyList() {
        List<Double> values = Collections.emptyList();
        double result = stats.exponentialMovingAverage(values, 0.3);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testExponentialMovingAverageWithSingleValue() {
        List<Double> values = Collections.singletonList(42.0);
        double result = stats.exponentialMovingAverage(values, 0.3);
        assertEquals(42.0, result, DELTA);
    }

    @Test
    void testMedianWithOddCount() {
        List<Double> values = Arrays.asList(1.0, 3.0, 5.0, 7.0, 9.0);
        double result = stats.median(values);
        assertEquals(5.0, result, DELTA);
    }

    @Test
    void testMedianWithEvenCount() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0);
        double result = stats.median(values);
        assertEquals(2.5, result, DELTA);
    }

    @Test
    void testMedianWithEmptyList() {
        List<Double> values = Collections.emptyList();
        double result = stats.median(values);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testMedianWithSingleValue() {
        List<Double> values = Collections.singletonList(42.0);
        double result = stats.median(values);
        assertEquals(42.0, result, DELTA);
    }

    @Test
    void testMean() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        double result = stats.mean(values);
        assertEquals(3.0, result, DELTA);
    }

    @Test
    void testMeanWithEmptyList() {
        List<Double> values = Collections.emptyList();
        double result = stats.mean(values);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testStandardDeviation() {
        List<Double> values = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        double result = stats.standardDeviation(values);
        assertTrue(result > 0);
    }

    @Test
    void testStandardDeviationWithSingleValue() {
        List<Double> values = Collections.singletonList(42.0);
        double result = stats.standardDeviation(values);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testStandardDeviationWithEmptyList() {
        List<Double> values = Collections.emptyList();
        double result = stats.standardDeviation(values);
        assertEquals(0.0, result, DELTA);
    }

    @Test
    void testIsWithinToleranceTrue() {
        assertTrue(stats.isWithinTolerance(105.0, 100.0, 10.0));
        assertTrue(stats.isWithinTolerance(95.0, 100.0, 10.0));
        assertTrue(stats.isWithinTolerance(100.0, 100.0, 10.0));
    }

    @Test
    void testIsWithinToleranceFalse() {
        assertFalse(stats.isWithinTolerance(115.0, 100.0, 10.0));
        assertFalse(stats.isWithinTolerance(85.0, 100.0, 10.0));
    }

    @Test
    void testIsWithinToleranceWithZeroTarget() {
        assertTrue(stats.isWithinTolerance(5.0, 0.0, 10.0));
        assertFalse(stats.isWithinTolerance(15.0, 0.0, 10.0));
    }

    @Test
    void testIsWithinToleranceWithNegativeValues() {
        assertTrue(stats.isWithinTolerance(-105.0, -100.0, 10.0));
        assertFalse(stats.isWithinTolerance(-115.0, -100.0, 10.0));
    }
}