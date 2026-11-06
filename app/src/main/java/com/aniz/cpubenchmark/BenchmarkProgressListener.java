package com.aniz.cpubenchmark;

/**
 * Functional Interface (SAM - Single Abstract Method).
 *
 * Demonstrates the direct relation between a Java Listener and a Kotlin Function:
 * - In Java: Implemented via anonymous class:
 *     engine.setOnProgressListener(new BenchmarkProgressListener() {
 *         public void onProgress(ThreadExecutionInfo info) { ... }
 *     });
 * - In Kotlin: SAM conversion enables passing a first-class function / lambda:
 *     engine.setOnProgressListener { info ->
 *         // Treated as a Kotlin function (ThreadExecutionInfo) -> Unit
 *     }
 */
@FunctionalInterface
public interface BenchmarkProgressListener {
    void onProgress(ThreadExecutionInfo info);
}
