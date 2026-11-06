package com.aniz.cpubenchmark

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * AnimatedVideoCanvasView - Interactive Animated Video Presentation of OS Concepts.
 *
 * Implements the "audio and video tutorial can be added - animated video" requirement.
 * Renders smooth 60fps animations illustrating:
 * Scene 0: Multi-Core Architecture (Heterogeneous big.LITTLE)
 * Scene 1: Linux CFS Thread Scheduling & Core Mapping (Requirement R3)
 * Scene 2: Race Conditions & Lost Updates
 * Scene 3: Amdahl's Law & Scalability Limits
 * Scene 4: Relation Between Functions & Listeners
 */
class AnimatedVideoCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var currentScene: Int = 0
        set(value) {
            field = value.coerceIn(0, 4)
            progress = 0f
            invalidate()
        }

    private var progress: Float = 0f // 0.0 to 1.0 within current scene
    private var isPlaying: Boolean = true

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 8000L // 8 seconds per scene loop
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#060B14") }
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0F1D33")
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2979FF")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPrimaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val textSecondaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#90A4C0")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }
    private val accentCyanPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        style = Paint.Style.FILL
    }
    private val accentGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64FFDA")
        style = Paint.Style.FILL
    }
    private val accentOrangePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9100")
        style = Paint.Style.FILL
    }
    private val alertRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        style = Paint.Style.FILL
    }

    init {
        animator.start()
    }

    fun play() {
        if (!isPlaying) {
            animator.start()
            isPlaying = true
        }
    }

    fun pause() {
        if (isPlaying) {
            animator.pause()
            isPlaying = false
        }
    }

    fun togglePlayPause(): Boolean {
        if (isPlaying) pause() else play()
        return isPlaying
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Draw scene-specific animation
        when (currentScene) {
            0 -> drawSceneMultiCore(canvas, w, h)
            1 -> drawSceneThreadScheduling(canvas, w, h)
            2 -> drawSceneRaceCondition(canvas, w, h)
            3 -> drawSceneAmdahl(canvas, w, h)
            4 -> drawSceneFunctionVsListener(canvas, w, h)
        }

        // Draw animated bottom timeline scrubber
        strokePaint.color = Color.parseColor("#17273F")
        strokePaint.strokeWidth = 6f
        canvas.drawLine(20f, h - 16f, w - 20f, h - 16f, strokePaint)

        strokePaint.color = Color.parseColor("#00E5FF")
        val sceneSegmentWidth = (w - 40f) / 5f
        val currentX = 20f + currentScene * sceneSegmentWidth + (progress * sceneSegmentWidth)
        canvas.drawLine(20f, h - 16f, currentX, h - 16f, strokePaint)
        canvas.drawCircle(currentX, h - 16f, 8f, accentCyanPaint)
    }

    // Scene 0: Multi-Core Architecture (Heterogeneous LITTLE vs BIG)
    private fun drawSceneMultiCore(canvas: Canvas, w: Float, h: Float) {
        textPrimaryPaint.textSize = 32f
        canvas.drawText("⚡ Scene 1: Multi-Core CPU Architecture", w / 2, 45f, textPrimaryPaint)
        textSecondaryPaint.textSize = 22f
        canvas.drawText("Heterogeneous Cores: Energy-Efficient LITTLE vs High-Power BIG", w / 2, 75f, textSecondaryPaint)

        // Draw CPU Chip boundary
        val chipRect = RectF(w * 0.12f, 100f, w * 0.88f, h - 45f)
        cardPaint.color = Color.parseColor("#0D1B2A")
        canvas.drawRoundRect(chipRect, 20f, 20f, cardPaint)
        strokePaint.color = Color.parseColor("#2979FF")
        strokePaint.strokeWidth = 4f
        canvas.drawRoundRect(chipRect, 20f, 20f, strokePaint)

        // Draw 8 cores (4 LITTLE, 4 BIG)
        val rows = 2
        val cols = 4
        val coreW = (chipRect.width() - 40f) / cols
        val coreH = (chipRect.height() - 40f) / rows

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = r * cols + c
                val left = chipRect.left + 20f + c * coreW
                val top = chipRect.top + 20f + r * coreH
                val rect = RectF(left + 6f, top + 6f, left + coreW - 6f, top + coreH - 6f)

                val isBig = idx >= 4
                val isPulsing = (progress * 8).toInt() == idx

                cardPaint.color = when {
                    isPulsing -> Color.parseColor("#1B3B6F")
                    isBig -> Color.parseColor("#241432")
                    else -> Color.parseColor("#0B2A36")
                }
                canvas.drawRoundRect(rect, 12f, 12f, cardPaint)

                strokePaint.color = if (isPulsing) Color.parseColor("#64FFDA")
                else if (isBig) Color.parseColor("#FF9100")
                else Color.parseColor("#00E5FF")
                strokePaint.strokeWidth = if (isPulsing) 5f else 2f
                canvas.drawRoundRect(rect, 12f, 12f, strokePaint)

                textPrimaryPaint.textSize = 22f
                canvas.drawText("Core $idx", rect.centerX(), rect.centerY() - 6f, textPrimaryPaint)
                textSecondaryPaint.textSize = 17f
                val typeLabel = if (isBig) "BIG (Perf)" else "LITTLE (Eff)"
                canvas.drawText(typeLabel, rect.centerX(), rect.centerY() + 18f, textSecondaryPaint)
            }
        }
    }

    // Scene 1: Thread Scheduling & Linux CFS Core Mapping (Requirement R3)
    private fun drawSceneThreadScheduling(canvas: Canvas, w: Float, h: Float) {
        textPrimaryPaint.textSize = 32f
        canvas.drawText("🧭 Scene 2: Thread Scheduling & Core Mapping (R3)", w / 2, 45f, textPrimaryPaint)
        textSecondaryPaint.textSize = 22f
        canvas.drawText("Linux CFS Scheduler dispatches threads onto specific hardware cores", w / 2, 75f, textSecondaryPaint)

        // Left box: RunQueue / Threads
        val queueRect = RectF(w * 0.06f, 110f, w * 0.36f, h - 50f)
        cardPaint.color = Color.parseColor("#0F1D33")
        canvas.drawRoundRect(queueRect, 16f, 16f, cardPaint)
        textPrimaryPaint.textSize = 20f
        canvas.drawText("OS Thread Queue", queueRect.centerX(), queueRect.top + 30f, textPrimaryPaint)

        // Center: Linux CFS Scheduler Arbiter
        val schedX = w * 0.52f
        val schedY = (110f + h - 50f) / 2
        cardPaint.color = Color.parseColor("#2979FF")
        canvas.drawCircle(schedX, schedY, 44f, cardPaint)
        textPrimaryPaint.textSize = 18f
        canvas.drawText("Linux CFS", schedX, schedY - 4f, textPrimaryPaint)
        textSecondaryPaint.textSize = 14f
        canvas.drawText("Scheduler", schedX, schedY + 16f, textSecondaryPaint)

        // Right box: CPU Hardware Cores
        val coresRect = RectF(w * 0.68f, 110f, w * 0.94f, h - 50f)
        cardPaint.color = Color.parseColor("#0B2A36")
        canvas.drawRoundRect(coresRect, 16f, 16f, cardPaint)
        textPrimaryPaint.textSize = 20f
        canvas.drawText("CPU Hardware", coresRect.centerX(), coresRect.top + 30f, textPrimaryPaint)

        // Draw animated thread traveling from queue -> CFS -> core
        val step = (progress * 4).toInt() % 4
        for (i in 0 until 4) {
            val ty = queueRect.top + 60f + i * 40f
            cardPaint.color = if (i == step) Color.parseColor("#00E5FF") else Color.parseColor("#17273F")
            val threadBox = RectF(queueRect.left + 10f, ty - 14f, queueRect.right - 10f, ty + 16f)
            canvas.drawRoundRect(threadBox, 8f, 8f, cardPaint)
            textPrimaryPaint.textSize = 16f
            canvas.drawText("Thread #$i [TID 238$i]", threadBox.centerX(), threadBox.centerY() + 5f, textPrimaryPaint)

            // Target core on right
            val cy = coresRect.top + 60f + i * 40f
            val coreBox = RectF(coresRect.left + 10f, cy - 14f, coresRect.right - 10f, cy + 16f)
            cardPaint.color = if (i == step) Color.parseColor("#64FFDA") else Color.parseColor("#17273F")
            canvas.drawRoundRect(coreBox, 8f, 8f, cardPaint)
            canvas.drawText("Core #$i (Active)", coreBox.centerX(), coreBox.centerY() + 5f, textPrimaryPaint)
        }

        // Animated traveling token
        val tParam = (progress * 4) - step
        val tokenX = queueRect.right + (coresRect.left - queueRect.right) * tParam.coerceIn(0f, 1f)
        val tokenY = schedY + sin(tParam * Math.PI.toFloat()) * 30f
        canvas.drawCircle(tokenX, tokenY, 10f, accentGreenPaint)
    }

    // Scene 2: Race Conditions & Lost Updates
    private fun drawSceneRaceCondition(canvas: Canvas, w: Float, h: Float) {
        textPrimaryPaint.textSize = 32f
        canvas.drawText("⚠️ Scene 3: Race Conditions & Critical Sections", w / 2, 45f, textPrimaryPaint)
        textSecondaryPaint.textSize = 22f
        canvas.drawText("Unsynchronized concurrent updates cause lost counts", w / 2, 75f, textSecondaryPaint)

        // Shared Memory Box in Center
        val memRect = RectF(w * 0.35f, 115f, w * 0.65f, 195f)
        cardPaint.color = Color.parseColor("#17273F")
        canvas.drawRoundRect(memRect, 16f, 16f, cardPaint)
        strokePaint.color = Color.parseColor("#00E5FF")
        canvas.drawRoundRect(memRect, 16f, 16f, strokePaint)

        textSecondaryPaint.textSize = 16f
        canvas.drawText("SHARED MEMORY (Counter)", memRect.centerX(), memRect.top + 26f, textSecondaryPaint)
        textPrimaryPaint.textSize = 34f
        val counterVal = if (progress < 0.6f) "count = 0" else "count = 1 (Expected: 2!)"
        canvas.drawText(counterVal, memRect.centerX(), memRect.centerY() + 18f, textPrimaryPaint)

        // Thread A on Left
        val threadARect = RectF(w * 0.08f, 220f, w * 0.44f, 320f)
        cardPaint.color = Color.parseColor("#0F1D33")
        canvas.drawRoundRect(threadARect, 14f, 14f, cardPaint)
        textPrimaryPaint.textSize = 20f
        canvas.drawText("Thread A", threadARect.centerX(), threadARect.top + 30f, textPrimaryPaint)
        textSecondaryPaint.textSize = 16f
        canvas.drawText("1. Reads 0  → 2. Writes 1", threadARect.centerX(), threadARect.centerY() + 18f, textSecondaryPaint)

        // Thread B on Right (Concurrent without lock!)
        val threadBRect = RectF(w * 0.56f, 220f, w * 0.92f, 320f)
        cardPaint.color = Color.parseColor("#0F1D33")
        canvas.drawRoundRect(threadBRect, 14f, 14f, cardPaint)
        textPrimaryPaint.textSize = 20f
        canvas.drawText("Thread B (Concurrent)", threadBRect.centerX(), threadBRect.top + 30f, textPrimaryPaint)
        textSecondaryPaint.textSize = 16f
        canvas.drawText("1. Reads 0  → 2. Overwrites 1!", threadBRect.centerX(), threadBRect.centerY() + 18f, textSecondaryPaint)

        // Lost update warning alert banner
        if (progress > 0.5f) {
            val alertRect = RectF(w * 0.15f, 335f, w * 0.85f, h - 45f)
            canvas.drawRoundRect(alertRect, 12f, 12f, alertRedPaint)
            textPrimaryPaint.textSize = 22f
            textPrimaryPaint.color = Color.WHITE
            canvas.drawText("🚨 RACE DETECTED: 1 Lost Update (50% Error)", alertRect.centerX(), alertRect.centerY() - 2f, textPrimaryPaint)
            textSecondaryPaint.textSize = 16f
            textSecondaryPaint.color = Color.WHITE
            canvas.drawText("Fix: Use AtomicLong or synchronized mutex lock", alertRect.centerX(), alertRect.centerY() + 22f, textSecondaryPaint)
            textSecondaryPaint.color = Color.parseColor("#90A4C0")
        }
    }

    // Scene 3: Amdahl's Law & Scalability Limits
    private fun drawSceneAmdahl(canvas: Canvas, w: Float, h: Float) {
        textPrimaryPaint.textSize = 32f
        canvas.drawText("📊 Scene 4: Amdahl's Law & Diminishing Returns", w / 2, 45f, textPrimaryPaint)
        textSecondaryPaint.textSize = 22f
        canvas.drawText("Formula: S(N) = 1 / ((1-P) + P/N) • Speedup is capped by serial fraction", w / 2, 75f, textSecondaryPaint)

        val startY = 115f
        val barH = 34f
        val configs = listOf(1, 2, 4, 8)

        for (i in configs.indices) {
            val cores = configs[i]
            val y = startY + i * 50f
            textPrimaryPaint.textSize = 18f
            canvas.drawText("${cores}T:", w * 0.08f, y + barH * 0.65f, textPrimaryPaint)

            val fullW = w * 0.76f
            val serialW = fullW * 0.15f // 15% serial overhead
            val parallelW = (fullW * 0.85f) / cores

            // Draw Serial portion (Red)
            val left = w * 0.14f
            val serialRect = RectF(left, y, left + serialW, y + barH)
            canvas.drawRoundRect(serialRect, 6f, 6f, alertRedPaint)

            // Draw Parallel portion (Cyan)
            val parallelRect = RectF(left + serialW + 4f, y, left + serialW + 4f + parallelW, y + barH)
            canvas.drawRoundRect(parallelRect, 6f, 6f, accentCyanPaint)

            val speedup = 1.0 / (0.15 + (0.85 / cores))
            textSecondaryPaint.textSize = 16f
            canvas.drawText(String.format("%.1fx speedup", speedup), left + serialW + parallelW + 55f, y + barH * 0.65f, textSecondaryPaint)
        }

        // Asymptote indicator
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 2f
        canvas.drawLine(w * 0.14f + (w * 0.76f * 0.15f), startY - 10f, w * 0.14f + (w * 0.76f * 0.15f), startY + 200f, strokePaint)
        textSecondaryPaint.textSize = 15f
        canvas.drawText("Serial Bottleneck (Limit: 1/0.15 = 6.67x max)", w * 0.58f, h - 50f, textSecondaryPaint)
    }

    // Scene 4: Relation Between Function and Listener
    private fun drawSceneFunctionVsListener(canvas: Canvas, w: Float, h: Float) {
        textPrimaryPaint.textSize = 32f
        canvas.drawText("🧩 Scene 5: Functions vs. Listeners in OS Architecture", w / 2, 45f, textPrimaryPaint)
        textSecondaryPaint.textSize = 22f
        canvas.drawText("Worker Thread (Java Listener) ➔ Looper ➔ Main Thread (Kotlin Lambda)", w / 2, 75f, textSecondaryPaint)

        // Box 1: Worker Thread
        val b1 = RectF(w * 0.06f, 120f, w * 0.32f, 260f)
        cardPaint.color = Color.parseColor("#0F1D33")
        canvas.drawRoundRect(b1, 14f, 14f, cardPaint)
        textPrimaryPaint.textSize = 18f
        canvas.drawText("Background Thread", b1.centerX(), b1.top + 28f, textPrimaryPaint)
        textSecondaryPaint.textSize = 15f
        canvas.drawText("Java Benchmark Engine", b1.centerX(), b1.top + 55f, textSecondaryPaint)
        canvas.drawText("Fires Event:", b1.centerX(), b1.centerY() + 15f, textSecondaryPaint)
        textPrimaryPaint.textSize = 16f
        canvas.drawText("listener.onProgress()", b1.centerX(), b1.centerY() + 40f, textPrimaryPaint)

        // Center: Android Looper / Message Queue
        val b2 = RectF(w * 0.38f, 120f, w * 0.62f, 260f)
        cardPaint.color = Color.parseColor("#17273F")
        canvas.drawRoundRect(b2, 14f, 14f, cardPaint)
        textPrimaryPaint.textSize = 18f
        canvas.drawText("Android Looper", b2.centerX(), b2.top + 28f, textPrimaryPaint)
        textSecondaryPaint.textSize = 15f
        canvas.drawText("MessageQueue", b2.centerX(), b2.top + 55f, textSecondaryPaint)
        canvas.drawText("Bridges Thread Boundary", b2.centerX(), b2.centerY() + 25f, textSecondaryPaint)

        // Box 3: Main Thread (Kotlin UI)
        val b3 = RectF(w * 0.68f, 120f, w * 0.94f, 260f)
        cardPaint.color = Color.parseColor("#0B2A36")
        canvas.drawRoundRect(b3, 14f, 14f, cardPaint)
        textPrimaryPaint.textSize = 18f
        canvas.drawText("Main UI Thread", b3.centerX(), b3.top + 28f, textPrimaryPaint)
        textSecondaryPaint.textSize = 15f
        canvas.drawText("Kotlin SAM Function", b3.centerX(), b3.top + 55f, textSecondaryPaint)
        canvas.drawText("Executes Lambda:", b3.centerX(), b3.centerY() + 15f, textSecondaryPaint)
        textPrimaryPaint.textSize = 16f
        canvas.drawText("{ info -> updateUI() }", b3.centerX(), b3.centerY() + 40f, textPrimaryPaint)

        // Animated event pulse traveling through pipeline
        val pulseX = b1.right + (b3.left - b1.right) * progress
        canvas.drawCircle(pulseX, 190f, 12f, accentGreenPaint)

        // Summary note at bottom
        val noteRect = RectF(w * 0.08f, 280f, w * 0.92f, h - 45f)
        cardPaint.color = Color.parseColor("#0F1D33")
        canvas.drawRoundRect(noteRect, 10f, 10f, cardPaint)
        textSecondaryPaint.textSize = 16f
        canvas.drawText("Key Takeaway: Listeners decouple thread lifecycle; Kotlin maps listeners into pure functions.", noteRect.centerX(), noteRect.centerY() + 5f, textSecondaryPaint)
    }
}
