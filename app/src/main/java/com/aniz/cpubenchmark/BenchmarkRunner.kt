package com.aniz.cpubenchmark

import android.os.Process
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

data class BenchmarkResult(
    val threadCount: Int,
    val elapsedMs: Long,
    val workUnits: Long,      // primes found, or matrix cells computed
    val threadIds: List<Int>,
    val threadExecutions: List<ThreadExecutionInfo> = emptyList(),
    val workloadType: WorkloadType = WorkloadType.PRIME_COUNTING,
    val schedulingPolicy: SchedulingPolicy = SchedulingPolicy.NORMAL
)

data class UnsafeResult(
    val elapsedMs: Long,
    val total: Long
)

class BenchmarkRunner {

    private fun doChunk(type: WorkloadType, start: Int, end: Int): Long = when (type) {
        WorkloadType.PRIME_COUNTING -> Workload.countPrimesInRange(start, end)
        WorkloadType.MATRIX_MULTIPLY -> Workload.multiplyRows(start, end)
    }

    // Correct, synchronized version — the real benchmark.
    fun run(
        threadCount: Int,
        rangeEnd: Int,
        type: WorkloadType = WorkloadType.PRIME_COUNTING,
        policy: SchedulingPolicy = SchedulingPolicy.NORMAL
    ): BenchmarkResult {
        val executor = Executors.newFixedThreadPool(threadCount)
        val total = AtomicLong(0)
        val tids = mutableListOf<Int>()
        val executions = mutableListOf<ThreadExecutionInfo>()
        val chunkSize = rangeEnd / threadCount

        val startTime = System.nanoTime()
        val futures = (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) rangeEnd else (i + 1) * chunkSize
            executor.submit {
                try {
                    Process.setThreadPriority(policy.nicePriority)
                } catch (ignored: Exception) {}

                val tid = Process.myTid()
                val initialCore = CpuTopologyHelper.getCpuForTid(tid, i)
                val cores = CpuTopologyHelper.getCpuCores()
                val coreType = if (initialCore in cores.indices) {
                    if (cores[initialCore].isBig) "BIG" else "LITTLE"
                } else "CORE"
                val priority = Process.getThreadPriority(tid)
                val schedPolicy = CpuTopologyHelper.getSchedulingPolicyName(tid)

                val info = ThreadExecutionInfo(i, tid, initialCore, coreType, priority, schedPolicy)
                synchronized(executions) { executions.add(info) }
                synchronized(tids) { tids.add(tid) }

                val chunkStart = System.currentTimeMillis()
                val result = doChunk(type, start, end)
                val chunkTime = System.currentTimeMillis() - chunkStart
                info.workUnits = result
                info.elapsedMs = chunkTime

                // Re-sample core upon completion to observe thread migration
                val endCore = CpuTopologyHelper.getCpuForTid(tid, initialCore)
                info.cpuCore = endCore

                total.addAndGet(result)
                Log.d("Benchmark", "TID=$tid handled [$start,$end) on Core $endCore -> $result in ${chunkTime}ms")
            }
        }
        futures.forEach { it.get() }
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
        executor.shutdown()

        return BenchmarkResult(threadCount, elapsedMs, total.get(), tids, executions, type, policy)
    }

    // Runs `run()` `repeats` times and averages the elapsed time to smooth out
    // OS scheduling jitter. Work units and TIDs come from the final run — the
    // workload is deterministic, so the work-unit count never changes between runs.
    fun runAveraged(
        threadCount: Int,
        rangeEnd: Int,
        type: WorkloadType = WorkloadType.PRIME_COUNTING,
        repeats: Int = 3,
        policy: SchedulingPolicy = SchedulingPolicy.NORMAL
    ): BenchmarkResult {
        var totalMs = 0L
        var last: BenchmarkResult? = null
        repeat(repeats) {
            val r = run(threadCount, rangeEnd, type, policy)
            totalMs += r.elapsedMs
            last = r
        }
        return last!!.copy(elapsedMs = totalMs / repeats)
    }

    // Deliberately unsafe — no synchronization on the shared accumulator.
    // Writes once per unit of work, so there are enough concurrent writes to
    // actually trigger a lost update (unlike writing once per thread).
    fun runUnsafe(
        threadCount: Int,
        rangeEnd: Int,
        type: WorkloadType = WorkloadType.PRIME_COUNTING
    ): UnsafeResult {
        var total = 0L
        val executor = Executors.newFixedThreadPool(threadCount)
        val chunkSize = rangeEnd / threadCount

        val startTime = System.nanoTime()
        val futures = (0 until threadCount).map { i ->
            val start = i * chunkSize
            val end = if (i == threadCount - 1) rangeEnd else (i + 1) * chunkSize
            executor.submit {
                when (type) {
                    WorkloadType.PRIME_COUNTING -> {
                        for (n in start until end) {
                            if (Workload.isPrime(n)) total++
                        }
                    }
                    WorkloadType.MATRIX_MULTIPLY -> {
                        val cells = Workload.multiplyRows(start, end)
                        for (c in 0L until cells) total++
                    }
                }
            }
        }
        futures.forEach { it.get() }
        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
        executor.shutdown()
        return UnsafeResult(elapsedMs, total)
    }
}
