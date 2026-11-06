package com.aniz.cpubenchmark;

import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CpuBenchmarkEngine - Pure Java Multi-Core Benchmarking Engine.
 *
 * Demonstrates:
 * 1. Java Concurrency: ExecutorService, Futures, AtomicLong synchronization.
 * 2. Operating System Scheduling (Requirement R3): Thread Priority / Nice values
 *    applied via android.os.Process.setThreadPriority, and CPU core affinity probing
 *    via /proc/self/task/[tid]/stat.
 * 3. Observer / Listener Pattern: Decoupled notification of worker thread lifecycle
 *    events to the UI via BenchmarkListener and BenchmarkProgressListener (SAM).
 * 4. Race Condition Demonstration: Safe atomic aggregation vs un-synchronized racy writes.
 */
public class CpuBenchmarkEngine {
    private static final String TAG = "CpuBenchmarkEngine";

    private BenchmarkListener listener;
    private BenchmarkProgressListener progressListener;

    public void setListener(BenchmarkListener listener) {
        this.listener = listener;
    }

    public void setOnProgressListener(BenchmarkProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private long executeChunk(WorkloadType type, int start, int end) {
        if (type == WorkloadType.PRIME_COUNTING) {
            return Workload.INSTANCE.countPrimesInRange(start, end);
        } else {
            return Workload.INSTANCE.multiplyRows(start, end);
        }
    }

    /**
     * Executes the benchmark across threadCount threads using the specified scheduling policy.
     */
    public BenchmarkResult runBenchmark(
            int threadCount,
            int rangeEnd,
            WorkloadType type,
            SchedulingPolicy policy
    ) {
        if (listener != null) {
            listener.onBenchmarkStarted(threadCount, type);
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicLong totalWorkUnits = new AtomicLong(0);
        List<ThreadExecutionInfo> executions = Collections.synchronizedList(new ArrayList<>());
        List<Integer> tids = Collections.synchronizedList(new ArrayList<>());
        int chunkSize = rangeEnd / threadCount;

        long startTime = System.nanoTime();
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            final int start = i * chunkSize;
            final int end = (i == threadCount - 1) ? rangeEnd : (i + 1) * chunkSize;

            futures.add(executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    int tid = threadIndex;
                    try {
                        Process.setThreadPriority(policy.getNicePriority());
                        tid = Process.myTid();
                    } catch (Throwable ignored) {
                        tid = (int) Thread.currentThread().getId();
                    }
                    tids.add(tid);

                    int initialCore = CpuTopologyHelper.getCpuForTid(tid, threadIndex);
                    List<CpuTopologyHelper.CpuCoreInfo> cores = CpuTopologyHelper.getCpuCores();
                    String coreType = "CORE";
                    if (initialCore >= 0 && initialCore < cores.size()) {
                        coreType = cores.get(initialCore).isBig ? "BIG" : "LITTLE";
                    }

                    int priority = 0;
                    try {
                        priority = Process.getThreadPriority(tid);
                    } catch (Throwable ignored) {
                        priority = Thread.currentThread().getPriority();
                    }
                    String schedPolicy = CpuTopologyHelper.getSchedulingPolicyName(tid);

                    ThreadExecutionInfo info = new ThreadExecutionInfo(
                            threadIndex, tid, initialCore, coreType, priority, schedPolicy
                    );
                    executions.add(info);

                    if (listener != null) {
                        listener.onThreadScheduled(info);
                    }
                    if (progressListener != null) {
                        progressListener.onProgress(info);
                    }

                    long chunkStart = System.currentTimeMillis();
                    long units = executeChunk(type, start, end);
                    long chunkElapsed = System.currentTimeMillis() - chunkStart;

                    info.setWorkUnits(units);
                    info.setElapsedMs(chunkElapsed);

                    // Re-sample CPU core to detect thread migration by Linux CFS
                    int endCore = CpuTopologyHelper.getCpuForTid(tid, initialCore);
                    info.setCpuCore(endCore);

                    totalWorkUnits.addAndGet(units);

                    if (listener != null) {
                        listener.onThreadProgress(threadIndex, 1.0f);
                    }
                    if (progressListener != null) {
                        progressListener.onProgress(info);
                    }

                    try {
                        Log.d(TAG, "Thread " + threadIndex + " [TID " + tid + "] ran on Core " +
                                endCore + " (" + coreType + "): units=" + units + " in " + chunkElapsed + "ms");
                    } catch (Throwable ignored) { }
                    return null;
                }
            }));
        }

        for (Future<Void> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                try {
                    Log.e(TAG, "Thread execution exception: " + e.getMessage());
                } catch (Throwable ignored) { }
            }
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
        executor.shutdown();

        BenchmarkResult result = new BenchmarkResult(
                threadCount,
                elapsedMs,
                totalWorkUnits.get(),
                new ArrayList<>(tids),
                new ArrayList<>(executions),
                type,
                policy
        );

        if (listener != null) {
            listener.onBenchmarkCompleted(result);
        }

        return result;
    }

    /**
     * Runs benchmark multiple times and returns the average elapsed time to eliminate scheduling jitter.
     */
    public BenchmarkResult runBenchmarkAveraged(
            int threadCount,
            int rangeEnd,
            WorkloadType type,
            SchedulingPolicy policy,
            int repeats
    ) {
        long totalMs = 0;
        BenchmarkResult last = null;
        for (int i = 0; i < repeats; i++) {
            last = runBenchmark(threadCount, rangeEnd, type, policy);
            totalMs += last.getElapsedMs();
        }
        if (last == null) {
            return new BenchmarkResult(threadCount, 0, 0, new ArrayList<>(), new ArrayList<>(), type, policy);
        }
        return new BenchmarkResult(
                threadCount,
                totalMs / repeats,
                last.getWorkUnits(),
                last.getThreadIds(),
                last.getThreadExecutions(),
                last.getWorkloadType(),
                last.getSchedulingPolicy()
        );
    }

    /**
     * Unsafe multi-threaded execution demonstrating race conditions and lost updates.
     */
    public UnsafeResult runUnsafeRaceCondition(
            int threadCount,
            int rangeEnd,
            WorkloadType type
    ) {
        final long[] sharedCounter = new long[]{0}; // Mutable without lock or atomic
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int chunkSize = rangeEnd / threadCount;

        long startTime = System.nanoTime();
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int start = i * chunkSize;
            final int end = (i == threadCount - 1) ? rangeEnd : (i + 1) * chunkSize;

            futures.add(executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    if (type == WorkloadType.PRIME_COUNTING) {
                        for (int n = start; n < end; n++) {
                            if (Workload.INSTANCE.isPrime(n)) {
                                // Direct unsynchronized write triggers lost updates
                                sharedCounter[0]++;
                            }
                        }
                    } else {
                        long cells = Workload.INSTANCE.multiplyRows(start, end);
                        for (long c = 0; c < cells; c++) {
                            sharedCounter[0]++;
                        }
                    }
                    return null;
                }
            }));
        }

        for (Future<Void> f : futures) {
            try {
                f.get();
            } catch (Exception ignored) { }
        }

        long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
        executor.shutdown();

        return new UnsafeResult(elapsedMs, sharedCounter[0]);
    }
}
