package com.aniz.cpubenchmark;

import java.util.Locale;

/**
 * Rigorous Implementation of Amdahl's Law for Multi-Core Benchmarking.
 *
 * Formula:
 *   Speedup S(N) = 1 / ((1 - P) + P / N)
 *
 * where:
 *   N = number of processing cores / threads
 *   P = parallelizable fraction of the workload (0.0 to 1.0)
 *   1 - P = sequential (serial) bottleneck (thread synchronization, chunk splitting, bus contention)
 *
 * As N -> infinity:
 *   Theoretical Maximum Speedup = 1 / (1 - P)
 */
public class AmdahlAnalysis {

    /**
     * Estimates the parallel fraction P from measured single-thread baseline and multi-thread time.
     * Derived from S = 1 / ((1 - P) + P / N):
     *   1 / S = 1 - P * (1 - 1/N)
     *   P = (1 - 1/S) / (1 - 1/N)
     */
    public static double estimateParallelFraction(long baselineMs, int threadCount, long measuredMs) {
        if (baselineMs <= 0 || measuredMs <= 0 || threadCount <= 1) {
            return 0.90; // Default empirical fallback (90% parallelizable)
        }
        double measuredSpeedup = (double) baselineMs / (double) measuredMs;
        double denom = 1.0 - (1.0 / threadCount);
        if (denom <= 0.0) return 0.90;

        double p = (1.0 - (1.0 / measuredSpeedup)) / denom;
        if (p < 0.0) p = 0.05;
        if (p > 0.999) p = 0.999;
        return p;
    }

    /**
     * Calculates theoretical speedup S(N) for N threads given parallel fraction P.
     */
    public static double theoreticalSpeedup(double parallelFraction, int threadCount) {
        if (threadCount <= 1) return 1.0;
        double serialFraction = 1.0 - parallelFraction;
        return 1.0 / (serialFraction + (parallelFraction / threadCount));
    }

    /**
     * Calculates theoretical maximum speedup limit as N -> infinity: 1 / (1 - P).
     */
    public static double theoreticalMaxSpeedup(double parallelFraction) {
        double serialFraction = 1.0 - parallelFraction;
        if (serialFraction <= 0.0001) return 999.0;
        return 1.0 / serialFraction;
    }

    /**
     * Calculates parallel efficiency percentage: (Speedup / N) * 100%.
     */
    public static double parallelEfficiency(double speedup, int threadCount) {
        if (threadCount <= 0) return 0.0;
        return (speedup / threadCount) * 100.0;
    }

    /**
     * Generates a detailed academic breakdown string for display in UI or logs.
     */
    public static String formatSummary(double parallelFraction, long baselineMs, int threads, long measuredMs) {
        double serialPct = (1.0 - parallelFraction) * 100.0;
        double parallelPct = parallelFraction * 100.0;
        double maxSpeedup = theoreticalMaxSpeedup(parallelFraction);
        double predictedSpeedup = theoreticalSpeedup(parallelFraction, threads);
        double actualSpeedup = (baselineMs > 0 && measuredMs > 0) ? ((double) baselineMs / measuredMs) : 1.0;
        double efficiency = parallelEfficiency(actualSpeedup, threads);

        return String.format(Locale.US,
                "Amdahl's Law Modeling:\n" +
                "• Formula: S(N) = 1 / ((1 - P) + P/N)\n" +
                "• Parallel Fraction (P): %.1f%%\n" +
                "• Serial Bottleneck (1 - P): %.1f%%\n" +
                "• Asymptotic Speedup Limit (N→∞): %.2fx\n" +
                "• At N = %d threads:\n" +
                "    - Theoretical Speedup: %.2fx\n" +
                "    - Measured Speedup:    %.2fx\n" +
                "    - Parallel Efficiency: %.1f%%",
                parallelPct, serialPct, maxSpeedup, threads, predictedSpeedup, actualSpeedup, efficiency);
    }
}
