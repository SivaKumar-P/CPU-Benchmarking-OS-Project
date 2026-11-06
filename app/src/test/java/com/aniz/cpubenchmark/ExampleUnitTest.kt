package com.aniz.cpubenchmark

import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class BenchmarkUnitTest {

    @Test
    fun testAmdahlAnalysisSpeedup() {
        val p = 0.90 // 90% parallel
        val s1 = AmdahlAnalysis.theoreticalSpeedup(p, 1)
        val s2 = AmdahlAnalysis.theoreticalSpeedup(p, 2)
        val s4 = AmdahlAnalysis.theoreticalSpeedup(p, 4)
        val s8 = AmdahlAnalysis.theoreticalSpeedup(p, 8)
        val sMax = AmdahlAnalysis.theoreticalMaxSpeedup(p)

        assertEquals(1.0, s1, 0.001)
        assertTrue("Speedup at 2T should be ~1.82x", s2 in 1.80..1.85)
        assertTrue("Speedup at 4T should be ~3.08x", s4 in 3.00..3.15)
        assertTrue("Speedup at 8T should be ~4.71x", s8 in 4.60..4.80)
        assertEquals(10.0, sMax, 0.001)
    }

    @Test
    fun testAmdahlParallelFractionEstimation() {
        val baseline = 1000L
        val measured2T = 550L
        val p = AmdahlAnalysis.estimateParallelFraction(baseline, 2, measured2T)
        assertTrue("Parallel fraction should be between 0.70 and 0.95", p in 0.70..0.95)
    }

    @Test
    fun testJavaEngineWithBenchmarkListener() {
        val engine = CpuBenchmarkEngine()
        val startedCalled = AtomicBoolean(false)
        val completedCalled = AtomicBoolean(false)

        engine.setListener(object : BenchmarkListener {
            override fun onBenchmarkStarted(totalThreads: Int, workloadType: WorkloadType) {
                startedCalled.set(true)
            }
            override fun onThreadScheduled(info: ThreadExecutionInfo) {}
            override fun onThreadProgress(threadIndex: Int, progressFraction: Float) {}
            override fun onBenchmarkCompleted(result: BenchmarkResult) {
                completedCalled.set(true)
            }
        })

        val result = engine.runBenchmark(2, 5000, WorkloadType.PRIME_COUNTING, SchedulingPolicy.NORMAL)

        assertTrue("onBenchmarkStarted should be called", startedCalled.get())
        assertTrue("onBenchmarkCompleted should be called", completedCalled.get())
        assertEquals(2, result.threadCount)
        assertTrue("Should have calculated work units", result.workUnits > 0)
    }

    @Test
    fun testRelationBetweenFunctionAndListener() {
        val engine = CpuBenchmarkEngine()
        var lambdaInvoked = false

        // Demonstrates Kotlin SAM conversion:
        // A Kotlin lambda function is passed directly where a Java BenchmarkProgressListener is expected!
        engine.setOnProgressListener { info ->
            assertNotNull(info)
            lambdaInvoked = true
        }

        val result = engine.runBenchmark(1, 1000, WorkloadType.PRIME_COUNTING, SchedulingPolicy.NORMAL)
        assertTrue("Kotlin lambda function should be invoked via Java SAM listener", lambdaInvoked)
        assertTrue(result.workUnits > 0)
    }

    @Test
    fun testCpuTopologyHelper() {
        val coreCount = CpuTopologyHelper.getAvailableProcessors()
        assertTrue("System should have at least 1 processor core", coreCount >= 1)
        val cores = CpuTopologyHelper.getCpuCores()
        assertEquals(coreCount, cores.size)
    }
}
