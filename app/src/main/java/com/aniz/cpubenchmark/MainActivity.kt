package com.aniz.cpubenchmark

import android.graphics.Color
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CombinedData
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val results = sortedMapOf<Int, Long>()
    private var baselineMs: Long? = null

    // UI elements
    private lateinit var tvCores: TextView
    private lateinit var tvProcessId: TextView
    private lateinit var llCpuCoresGrid: LinearLayout
    private lateinit var tvSchedulingModeDesc: TextView
    private lateinit var rgSchedulingPolicy: RadioGroup
    private lateinit var tvCoreMappingLog: TextView

    private lateinit var btnOpenTutorial: Button
    private lateinit var btnOpenStudyGuide: Button

    private lateinit var rgEngine: RadioGroup
    private lateinit var rgWorkload: RadioGroup
    private lateinit var tvWorkloadNote: TextView
    private lateinit var rgThreads: RadioGroup
    private lateinit var etRange: EditText

    private lateinit var btnRun: Button
    private lateinit var btnRunAll: Button
    private lateinit var btnCompareSafety: Button
    private lateinit var btnRaceDemo: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var tvAmdahlSummary: TextView
    private lateinit var tvAmdahlSliderLabel: TextView
    private lateinit var sbAmdahlParallel: SeekBar
    private lateinit var tvAmdahlSliderProjection: TextView

    private lateinit var tvLog: TextView
    private lateinit var barChart: CombinedChart

    // Requirement R3 Core View holders
    private data class CoreViewHolder(
        val container: LinearLayout,
        val tvCoreTitle: TextView,
        val tvCoreSub: TextView,
        val tvThreadBadge: TextView
    )
    private val coreViewHolders = mutableListOf<CoreViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupCpuTopologyAndGrid()
        setupChartStyle()
        setupListeners()
        setupAmdahlSimulator()
    }

    private fun bindViews() {
        tvCores = findViewById(R.id.tvCores)
        tvProcessId = findViewById(R.id.tvProcessId)
        llCpuCoresGrid = findViewById(R.id.llCpuCoresGrid)
        tvSchedulingModeDesc = findViewById(R.id.tvSchedulingModeDesc)
        rgSchedulingPolicy = findViewById(R.id.rgSchedulingPolicy)
        tvCoreMappingLog = findViewById(R.id.tvCoreMappingLog)

        btnOpenTutorial = findViewById(R.id.btnOpenTutorial)
        btnOpenStudyGuide = findViewById(R.id.btnOpenStudyGuide)

        rgEngine = findViewById(R.id.rgEngine)
        rgWorkload = findViewById(R.id.rgWorkload)
        tvWorkloadNote = findViewById(R.id.tvWorkloadNote)
        rgThreads = findViewById(R.id.rgThreads)
        etRange = findViewById(R.id.etRange)

        btnRun = findViewById(R.id.btnRun)
        btnRunAll = findViewById(R.id.btnRunAll)
        btnCompareSafety = findViewById(R.id.btnCompareSafety)
        btnRaceDemo = findViewById(R.id.btnRaceDemo)
        progressBar = findViewById(R.id.progressBar)

        tvAmdahlSummary = findViewById(R.id.tvAmdahlSummary)
        tvAmdahlSliderLabel = findViewById(R.id.tvAmdahlSliderLabel)
        sbAmdahlParallel = findViewById(R.id.sbAmdahlParallel)
        tvAmdahlSliderProjection = findViewById(R.id.tvAmdahlSliderProjection)

        tvLog = findViewById(R.id.tvLog)
        barChart = findViewById(R.id.barChart)

        tvProcessId.text = "PID: ${Process.myPid()}"
    }

    private fun setupCpuTopologyAndGrid() {
        val cores = CpuTopologyHelper.getCpuCores()
        val totalCores = cores.size
        tvCores.text = "🖥 Detected CPU: $totalCores Hardware Cores (big.LITTLE Topology)"

        llCpuCoresGrid.removeAllViews()
        coreViewHolders.clear()

        val columns = if (totalCores >= 8) 4 else 2
        var currentRow: LinearLayout? = null

        cores.forEachIndexed { index, coreInfo ->
            if (index % columns == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = 8
                    }
                }
                llCpuCoresGrid.addView(currentRow)
            }

            // Create individual Core Chip Card
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_core_idle)
                setPadding(12, 12, 12, 12)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1.0f
                ).apply {
                    if (index % columns != 0) leftMargin = 8
                }
            }

            val tvTitle = TextView(this).apply {
                text = "Core $index"
                setTextColor(Color.WHITE)
                textSize = 12f
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val tvSub = TextView(this).apply {
                val typeStr = if (coreInfo.isBig) "BIG (Perf)" else "LITTLE (Eff)"
                val freqStr = if (coreInfo.maxFreqKhz > 0) String.format(Locale.US, " • %.1fG", coreInfo.maxFreqKhz / 1_000_000.0) else ""
                text = "$typeStr$freqStr"
                setTextColor(Color.parseColor("#90A4C0"))
                textSize = 10f
            }

            val tvBadge = TextView(this).apply {
                text = "Idle"
                setTextColor(Color.parseColor("#64FFDA"))
                textSize = 10f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 4, 0, 0)
            }

            card.addView(tvTitle)
            card.addView(tvSub)
            card.addView(tvBadge)
            currentRow?.addView(card)

            coreViewHolders.add(CoreViewHolder(card, tvTitle, tvSub, tvBadge))
        }
    }

    private fun setupListeners() {
        btnOpenTutorial.setOnClickListener {
            TutorialBottomSheetDialog.newInstance().show(supportFragmentManager, TutorialBottomSheetDialog.TAG)
        }

        btnOpenStudyGuide.setOnClickListener {
            StudyGuideBottomSheetDialog.newInstance().show(supportFragmentManager, StudyGuideBottomSheetDialog.TAG)
        }

        rgWorkload.setOnCheckedChangeListener { _, checkedId ->
            val isMatrix = checkedId == R.id.rbMatrix
            etRange.isEnabled = !isMatrix
            tvWorkloadNote.text = if (isMatrix)
                "Matrix mode: fixed ${Workload.MATRIX_SIZE}×${Workload.MATRIX_SIZE} matrices (range field ignored)"
            else
                "Prime mode: counts primes up to the range above"
        }

        rgSchedulingPolicy.setOnCheckedChangeListener { _, checkedId ->
            val policy = getSelectedSchedulingPolicy()
            tvSchedulingModeDesc.text = when (policy) {
                SchedulingPolicy.NORMAL ->
                    "Normal Priority (Nice 0): Standard Linux Completely Fair Scheduler (CFS) time-sharing."
                SchedulingPolicy.BACKGROUND ->
                    "Background Priority (Nice +10): Low-priority time slice. Threads receive fewer CPU cycles, yielding to foreground apps."
                SchedulingPolicy.URGENT ->
                    "Urgent Priority (Nice -16): High-priority execution slice. CFS preemptively schedules threads onto performance cores."
            }
        }

        btnRun.setOnClickListener { runBenchmark() }
        btnRunAll.setOnClickListener { runAllBenchmarks() }
        btnCompareSafety.setOnClickListener { compareSafety() }
        btnRaceDemo.setOnClickListener { runRaceDemo() }
    }

    private fun setupAmdahlSimulator() {
        sbAmdahlParallel.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Maps 0..50 to 50% .. 99%
                val p = 0.50 + (progress * 0.49 / 50.0)
                val pPct = p * 100.0
                tvAmdahlSliderLabel.text = String.format(Locale.US, "Interactive Simulator: Parallel Fraction (P) = %.1f%%", pPct)

                val s2 = AmdahlAnalysis.theoreticalSpeedup(p, 2)
                val s4 = AmdahlAnalysis.theoreticalSpeedup(p, 4)
                val s8 = AmdahlAnalysis.theoreticalSpeedup(p, 8)
                val s16 = AmdahlAnalysis.theoreticalSpeedup(p, 16)
                val sMax = AmdahlAnalysis.theoreticalMaxSpeedup(p)

                tvAmdahlSliderProjection.text = String.format(
                    Locale.US,
                    "At P = %.1f%%:  2T = %.2fx | 4T = %.2fx | 8T = %.2fx | 16T = %.2fx | Max (∞) = %.2fx",
                    pPct, s2, s4, s8, s16, sMax
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        // Trigger initial calculation
        sbAmdahlParallel.progress = 40
    }

    private fun getSelectedThreadCount(): Int = when (rgThreads.checkedRadioButtonId) {
        R.id.rb1 -> 1
        R.id.rb2 -> 2
        R.id.rb4 -> 4
        R.id.rb8 -> 8
        else -> 1
    }

    private fun selectedWorkloadType(): WorkloadType =
        if (rgWorkload.checkedRadioButtonId == R.id.rbMatrix) WorkloadType.MATRIX_MULTIPLY
        else WorkloadType.PRIME_COUNTING

    private fun getSelectedSchedulingPolicy(): SchedulingPolicy = when (rgSchedulingPolicy.checkedRadioButtonId) {
        R.id.rbSchedBackground -> SchedulingPolicy.BACKGROUND
        R.id.rbSchedUrgent -> SchedulingPolicy.URGENT
        else -> SchedulingPolicy.NORMAL
    }ah

    private fun isJavaEngineSelected(): Boolean =
        rgEngine.checkedRadioButtonId == R.id.rbEngineJava

    private fun resetCoreHighlights() {
        runOnUiThread {
            coreViewHolders.forEach { holder ->
                holder.container.background = ContextCompat.getDrawable(this, R.drawable.bg_core_idle)
                holder.tvThreadBadge.text = "Idle"
                holder.tvThreadBadge.setTextColor(Color.parseColor("#90A4C0"))
            }
        }
    }

    private fun highlightCore(coreIndex: Int, threadIndex: Int, tid: Int) {
        runOnUiThread {
            if (coreIndex in coreViewHolders.indices) {
                val holder = coreViewHolders[coreIndex]
                holder.container.background = ContextCompat.getDrawable(this, R.drawable.bg_core_active)
                holder.tvThreadBadge.text = "🧵 T$threadIndex [TID $tid]"
                holder.tvThreadBadge.setTextColor(Color.parseColor("#64FFDA"))
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val visibility = if (enabled) View.VISIBLE else View.GONE
        btnRun.visibility = visibility
        btnRunAll.visibility = visibility
        btnRaceDemo.visibility = visibility
        btnCompareSafety.visibility = visibility
        btnRun.isEnabled = enabled
        btnRunAll.isEnabled = enabled
        btnRaceDemo.isEnabled = enabled
        btnCompareSafety.isEnabled = enabled
    }

    private fun speedupText(elapsedMs: Long): String {
        val base = baselineMs ?: return "— (run 1 thread first)"
        return String.format(Locale.US, "%.2fx", base.toDouble() / elapsedMs.toDouble())
    }

    private fun unitLabelFor(type: WorkloadType) =
        if (type == WorkloadType.PRIME_COUNTING) "Primes" else "Cells"

    private fun updateAmdahlAnalysis(threadCount: Int, measuredMs: Long) {
        val base = baselineMs ?: return
        if (threadCount <= 1 || measuredMs <= 0) return

        val p = AmdahlAnalysis.estimateParallelFraction(base, threadCount, measuredMs)
        val summary = AmdahlAnalysis.formatSummary(p, base, threadCount, measuredMs)
        tvAmdahlSummary.text = summary

        // Update slider position to match estimated parallel fraction
        val progressVal = ((p - 0.50) / 0.49 * 50.0).toInt().coerceIn(0, 50)
        sbAmdahlParallel.progress = progressVal
    }

    private fun runBenchmark() {
        val threadCount = getSelectedThreadCount()
        val type = selectedWorkloadType()
        val policy = getSelectedSchedulingPolicy()
        val range = Workload.rangeEndFor(type, etRange.text.toString().toIntOrNull() ?: 5_000_000)
        val useJava = isJavaEngineSelected()

        setButtonsEnabled(false)
        resetCoreHighlights()
        progressBar.visibility = View.VISIBLE
        val engineName = if (useJava) "Java Engine (with Listeners)" else "Kotlin Engine (with Functions)"
        tvLog.text = "Running $threadCount thread(s) via $engineName [${policy.label}]..."
        tvCoreMappingLog.text = "Dispatching $threadCount worker threads to OS scheduler..."

        Thread {
            val result: BenchmarkResult = if (useJava) {
                // Pure Java Engine execution with Java BenchmarkListener
                val engine = CpuBenchmarkEngine()

                // Demonstrate traditional Java Listener
                engine.setListener(object : BenchmarkListener {
                    override fun onBenchmarkStarted(totalThreads: Int, workloadType: WorkloadType) {
                        runOnUiThread {
                            tvCoreMappingLog.text = "Benchmark started with $totalThreads threads..."
                        }
                    }

                    override fun onThreadScheduled(info: ThreadExecutionInfo) {
                        highlightCore(info.cpuCore, info.threadIndex, info.tid)
                    }

                    override fun onThreadProgress(threadIndex: Int, progressFraction: Float) {
                        // Progress update
                    }

                    override fun onBenchmarkCompleted(res: BenchmarkResult) {
                        // Handled upon return
                    }
                })

                // Demonstrate SAM Functional Interface conversion (Function vs Listener relation)
                engine.setOnProgressListener { info ->
                    highlightCore(info.cpuCore, info.threadIndex, info.tid)
                }

                engine.runBenchmarkAveraged(threadCount, range, type, policy, 3)
            } else {
                // Kotlin execution
                val runner = BenchmarkRunner()
                val r = runner.runAveraged(threadCount, range, type, 3, policy)
                r.threadExecutions.forEach { info ->
                    highlightCore(info.cpuCore, info.threadIndex, info.tid)
                }
                r
            }

            if (threadCount == 1) baselineMs = result.elapsedMs
            results[threadCount] = result.elapsedMs

            runOnUiThread {
                val executions = result.threadExecutions
                val mappingSb = StringBuilder("🧵 Requirement R3 Execution & Core Mapping (${result.threadCount} threads):\n")
                if (executions.isNotEmpty()) {
                    executions.forEach { info ->
                        mappingSb.append("• Thread #${info.threadIndex} [TID ${info.tid}] ➔ Core ${info.cpuCore} (${info.coreType}, Nice: ${info.priority}, ${info.schedulingPolicy})\n")
                        highlightCore(info.cpuCore, info.threadIndex, info.tid)
                    }
                } else {
                    result.threadIds.forEachIndexed { i, tid ->
                        val core = CpuTopologyHelper.getCpuForTid(tid, i)
                        mappingSb.append("• Thread #$i [TID $tid] ➔ Core $core\n")
                        highlightCore(core, i, tid)
                    }
                }
                tvCoreMappingLog.text = mappingSb.toString().trim()

                tvLog.text = "Threads=$threadCount  Avg Time=${result.elapsedMs}ms  " +
                        "${unitLabelFor(type)}=${result.workUnits}  Speedup=${speedupText(result.elapsedMs)}\n" +
                        "Engine: ${if (useJava) "Java Engine (Listeners)" else "Kotlin Engine (Functions)"}\n" +
                        "TIDs used: ${result.threadIds}"

                updateAmdahlAnalysis(threadCount, result.elapsedMs)
                updateChart()
                progressBar.visibility = View.GONE
                setButtonsEnabled(true)
            }
        }.start()
    }

    private fun runAllBenchmarks() {

        
        val type = selectedWorkloadType()
        val policy = getSelectedSchedulingPolicy()
        val range = Workload.rangeEndFor(type, etRange.text.toString().toIntOrNull() ?: 5_000_000)
        val configs = listOf(1, 2, 4, 8)
        val useJava = isJavaEngineSelected()

        setButtonsEnabled(false)
        resetCoreHighlights()
        progressBar.visibility = View.VISIBLE
        tvLog.text = "Running full sweep (1 → 2 → 4 → 8 threads) with ${if (useJava) "Java Engine" else "Kotlin Engine"}..."

        Thread {
            for (tc in configs) {
                val result: BenchmarkResult = if (useJava) {
                    val engine = CpuBenchmarkEngine()
                    engine.setOnProgressListener { info ->
                        highlightCore(info.cpuCore, info.threadIndex, info.tid)
                    }
                    engine.runBenchmarkAveraged(tc, range, type, policy, 3)
                } else {
                    val runner = BenchmarkRunner()
                    val r = runner.runAveraged(tc, range, type, 3, policy)
                    r.threadExecutions.forEach { info ->
                        highlightCore(info.cpuCore, info.threadIndex, info.tid)
                    }
                    r
                }

                if (tc == 1) baselineMs = result.elapsedMs
                results[tc] = result.elapsedMs

                runOnUiThread {
                    val mappingSb = StringBuilder("🧵 Requirement R3 Live Thread-to-Core Allocation ($tc threads):\n")
                    result.threadExecutions.forEach { info ->
                        mappingSb.append("• Thread #${info.threadIndex} [TID ${info.tid}] ➔ Core ${info.cpuCore} (${info.coreType}, Nice: ${info.priority})\n")
                        highlightCore(info.cpuCore, info.threadIndex, info.tid)
                    }
                    tvCoreMappingLog.text = mappingSb.toString().trim()

                    tvLog.text = "Threads=$tc  Avg Time=${result.elapsedMs}ms  " +
                            "${unitLabelFor(type)}=${result.workUnits}  Speedup=${speedupText(result.elapsedMs)}"
                    updateAmdahlAnalysis(tc, result.elapsedMs)
                    updateChart()
                }
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                setButtonsEnabled(true)
            }
        }.start()
    }

    private fun runRaceDemo() {
        val type = selectedWorkloadType()
        val range = Workload.rangeEndFor(type, etRange.text.toString().toIntOrNull() ?: 5_000_000)

        setButtonsEnabled(false)
        resetCoreHighlights()
        progressBar.visibility = View.VISIBLE
        tvLog.text = "Running unsafe race-condition demo (8 threads, 5 consecutive runs)..."

        Thread {
            val runner = BenchmarkRunner()
            val runs = (1..5).map { runner.runUnsafe(8, range, type).total }
            val distinctCounts = runs.distinct().size
            runOnUiThread {
                tvLog.text = "Unsafe totals across 5 runs:\n$runs\n" +
                        if (distinctCounts > 1) {
                            "🚨 RACE CONDITION VERIFIED! Multiple different totals occurred due to unsynchronized lost updates."
                        } else {
                            "Completed 5 runs. Totals: $runs (run again or increase range to observe lost updates)."
                        }
                progressBar.visibility = View.GONE
                setButtonsEnabled(true)
            }
        }.start()
    }

    private fun compareSafety() {
        val type = selectedWorkloadType()
        val range = Workload.rangeEndFor(type, etRange.text.toString().toIntOrNull() ?: 5_000_000)

        setButtonsEnabled(false)
        resetCoreHighlights()
        progressBar.visibility = View.VISIBLE
        tvLog.text = "Comparing safe vs unsafe execution at 8 threads..."

        Thread {
            val runner = BenchmarkRunner()
            val safe = runner.run(8, range, type)
            val unsafe = runner.runUnsafe(8, range, type)
            val overheadPct = if (unsafe.elapsedMs > 0)
                ((safe.elapsedMs - unsafe.elapsedMs).toDouble() / unsafe.elapsedMs.toDouble()) * 100
            else 0.0
            val mismatch = safe.workUnits != unsafe.total
            val lostCount = safe.workUnits - unsafe.total

            runOnUiThread {
                val errorMsg = if (mismatch) {
                    "\n🚨 RACE TRIGGERED: Lost $lostCount updates (${String.format(Locale.US, "%.2f", (lostCount.toDouble() / safe.workUnits) * 100)}% data loss!)"
                } else {
                    "\n(Totals matched on this run; thread interleaving did not collide)."
                }

                tvLog.text = "• Safe (AtomicLong):  Time=${safe.elapsedMs}ms  Total=${safe.workUnits}\n" +
                        "• Unsafe (Unsynced): Time=${unsafe.elapsedMs}ms  Total=${unsafe.total}\n" +
                        "• Synchronization Overhead: ${String.format(Locale.US, "%.1f", overheadPct)}%" +
                        errorMsg
                progressBar.visibility = View.GONE
                setButtonsEnabled(true)
            }
        }.start()
    }

    private fun setupChartStyle() {
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.legend.textColor = Color.WHITE
        barChart.axisLeft.textColor = Color.WHITE
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
        barChart.xAxis.textColor = Color.WHITE
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setDrawGridLines(false)
        barChart.setDrawValueAboveBar(true)
        barChart.drawOrder = arrayOf(CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE)
        barChart.setNoDataText("Run a benchmark to see results")
        barChart.setNoDataTextColor(Color.WHITE)
    }

    private fun updateChart() {
        val threadCounts = results.keys.toList()
        val palette = listOf(
            Color.parseColor("#2979FF"),
            Color.parseColor("#00E5FF"),
            Color.parseColor("#00BFA5"),
            Color.parseColor("#64FFDA")
        )
        val barEntries = threadCounts.mapIndexed { index, tc ->
            BarEntry(index.toFloat(), results[tc]!!.toFloat())
        }
        val labels = threadCounts.map { "${it}t" }

        val barDataSet = BarDataSet(barEntries, "Measured time (ms)")
        barDataSet.colors = threadCounts.indices.map { palette[it % palette.size] }
        barDataSet.valueTextColor = Color.WHITE
        barDataSet.valueTextSize = 11f
        val barData = BarData(barDataSet)
        barData.barWidth = 0.5f

        val combinedData = CombinedData()
        combinedData.setData(barData)

        val base = baselineMs
        if (base != null && threadCounts.any { it > 1 }) {
            val maxThread = threadCounts.filter { it > 1 }.maxOrNull() ?: 2
            val measuredTime = results[maxThread] ?: base
            val p = AmdahlAnalysis.estimateParallelFraction(base, maxThread, measuredTime)

            val lineEntries = threadCounts.mapIndexed { index, tc ->
                val predictedSpeedup = AmdahlAnalysis.theoreticalSpeedup(p, tc)
                val predictedMs = base / predictedSpeedup
                Entry(index.toFloat(), predictedMs.toFloat())
            }
            val lineDataSet = LineDataSet(lineEntries, "Amdahl Prediction (P=${String.format(Locale.US, "%.0f", p * 100)}%)")
            lineDataSet.color = Color.WHITE
            lineDataSet.setDrawCircles(true)
            lineDataSet.circleRadius = 4f
            lineDataSet.setCircleColor(Color.WHITE)
            lineDataSet.lineWidth = 2f
            lineDataSet.enableDashedLine(12f, 6f, 0f)
            lineDataSet.setDrawValues(false)
            combinedData.setData(LineData(lineDataSet))
        }

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.axisMinimum = -0.5f
        barChart.xAxis.axisMaximum = threadCounts.size - 0.5f
        barChart.data = combinedData
        barChart.animateY(500)
        barChart.invalidate()
    }
}
