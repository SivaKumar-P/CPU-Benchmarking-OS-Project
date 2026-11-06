package com.aniz.cpubenchmark;

/**
 * BenchmarkListener - Traditional Java Observer / Event Listener pattern.
 *
 * Demonstrates:
 * 1. Java Listener Pattern: An interface defining callback events across thread boundaries.
 * 2. Relation Between Function & Listener:
 *    - In traditional Java, asynchronous worker threads deliver updates to callers via
 *      Listener interface callbacks.
 *    - In Kotlin, Single Abstract Method (SAM) conversion or lambdas/function types
 *      can be used seamlessly in place of single-method listeners.
 *    - Multi-method listeners require implementing each lifecycle event, separating concerns
 *      (e.g., thread scheduling vs. progress updates vs. benchmark completion).
 */
public interface BenchmarkListener {
    /**
     * Fired when the multi-threaded benchmark commences.
     */
    void onBenchmarkStarted(int totalThreads, WorkloadType workloadType);

    /**
     * Fired when a thread is dispatched and bound to a CPU core.
     * Essential for Requirement R3 (core highlighting and thread scheduling).
     */
    void onThreadScheduled(ThreadExecutionInfo info);

    /**
     * Fired periodically as a thread processes its assigned work chunk.
     */
    void onThreadProgress(int threadIndex, float progressFraction);

    /**
     * Fired when all concurrent worker threads complete their execution.
     */
    void onBenchmarkCompleted(BenchmarkResult result);
}
