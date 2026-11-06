package com.aniz.cpubenchmark

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TutorialBottomSheetDialog : BottomSheetDialogFragment() {

    private lateinit var canvas: AnimatedVideoCanvasView
    private lateinit var tvSceneTitle: TextView
    private lateinit var tvNarrationText: TextView
    private lateinit var btnPlayPause: Button
    private lateinit var btnPrevScene: Button
    private lateinit var btnNextScene: Button
    private lateinit var btnMuteAudio: Button

    private var audioManager: TutorialAudioManager? = null

    private val sceneNarrations = listOf(
        "Scene 1: Multi-Core CPU Hardware Architecture.\n" +
                "Modern mobile processors contain heterogeneous CPU cores. Energy-efficient LITTLE cores handle background tasks with low power draw, while high-performance BIG cores handle compute-intensive benchmarks. Our app measures how your device balances threads across these cores.",

        "Scene 2: Thread Scheduling and Core Mapping (Requirement R3).\n" +
                "In Android and Linux, threads are 1-to-1 kernel tasks scheduled by the Completely Fair Scheduler (CFS). The kernel dynamically assigns each thread to a physical CPU core. By inspecting /proc/self/task/TID/stat, our benchmark detects and visually highlights which core runs which thread.",

        "Scene 3: Race Conditions and Critical Sections.\n" +
                "When multiple threads concurrently read, modify, and write shared memory without synchronization, race conditions occur. As shown in the animation, concurrent increments cause lost updates. Using atomic variables or synchronized locks protects the critical section and guarantees correctness.",

        "Scene 4: Amdahl's Law and Scalability Limits.\n" +
                "Amdahl's Law states that program speedup is strictly bounded by its sequential portion. As more CPU cores are added, the parallel portion executes faster, but the serial bottleneck remains unchanged. The theoretical maximum speedup is strictly limited to 1 divided by (1 - P).",

        "Scene 5: Functions vs. Listeners in Asynchronous Architecture.\n" +
                "In Java, event listeners are interfaces used for decoupled asynchronous callbacks across thread boundaries. In Kotlin, Single Abstract Method (SAM) conversions treat listeners as first-class functions. Listeners allow worker threads to safely report results back to the Android UI main thread."
    )

    private val sceneTitles = listOf(
        "Scene 1 of 5: Multi-Core Hardware & Heterogeneous Cores",
        "Scene 2 of 5: Thread Scheduling & Core Mapping (Requirement R3)",
        "Scene 3 of 5: Race Conditions & Critical Sections",
        "Scene 4 of 5: Amdahl's Law & Scalability Limits",
        "Scene 5 of 5: Relation Between Functions & Listeners"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_tutorial, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = true
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        canvas = view.findViewById(R.id.animatedVideoCanvas)
        tvSceneTitle = view.findViewById(R.id.tvSceneTitle)
        tvNarrationText = view.findViewById(R.id.tvNarrationText)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnPrevScene = view.findViewById(R.id.btnPrevScene)
        btnNextScene = view.findViewById(R.id.btnNextScene)
        btnMuteAudio = view.findViewById(R.id.btnMuteAudio)

        context?.let { ctx ->
            audioManager = TutorialAudioManager(ctx).apply {
                onSpeechCompleted = {
                    activity?.runOnUiThread {
                        // Optional auto-progression or loop
                    }
                }
            }
        }

        updateSceneDisplay(0)

        btnPlayPause.setOnClickListener {
            val isPlaying = canvas.togglePlayPause()
            btnPlayPause.text = if (isPlaying) "⏸ Pause" else "▶ Play"
            if (!isPlaying) {
                audioManager?.stopNarration()
            } else {
                playCurrentNarration()
            }
        }

        btnPrevScene.setOnClickListener {
            if (canvas.currentScene > 0) {
                updateSceneDisplay(canvas.currentScene - 1)
            }
        }

        btnNextScene.setOnClickListener {
            if (canvas.currentScene < 4) {
                updateSceneDisplay(canvas.currentScene + 1)
            }
        }

        btnMuteAudio.setOnClickListener {
            audioManager?.let { am ->
                val newMuted = !am.isMuted()
                am.setMuted(newMuted)
                btnMuteAudio.text = if (newMuted) "🔇 Muted" else "🔊 Voice On"
            }
        }

        view.findViewById<Button>(R.id.chipScene0).setOnClickListener { updateSceneDisplay(0) }
        view.findViewById<Button>(R.id.chipScene1).setOnClickListener { updateSceneDisplay(1) }
        view.findViewById<Button>(R.id.chipScene2).setOnClickListener { updateSceneDisplay(2) }
        view.findViewById<Button>(R.id.chipScene3).setOnClickListener { updateSceneDisplay(3) }
        view.findViewById<Button>(R.id.chipScene4).setOnClickListener { updateSceneDisplay(4) }
    }

    private fun updateSceneDisplay(sceneIndex: Int) {
        canvas.currentScene = sceneIndex
        tvSceneTitle.text = sceneTitles[sceneIndex]
        tvNarrationText.text = sceneNarrations[sceneIndex]
        audioManager?.playToneCue()
        playCurrentNarration()
    }

    private fun playCurrentNarration() {
        val text = sceneNarrations[canvas.currentScene]
        audioManager?.speakNarration(text, "scene_${canvas.currentScene}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        audioManager?.release()
        audioManager = null
    }

    companion object {
        const val TAG = "TutorialBottomSheetDialog"
        fun newInstance() = TutorialBottomSheetDialog()
    }
}
