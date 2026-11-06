# Multi-Core CPU Benchmarking & OS Monitor (Android)

An Android application written in **Java** and **Kotlin** that investigates low-level Linux OS kernel scheduling, hardware core affinity (**Requirement R3**), Amdahl's Law, and concurrency hazards on heterogeneous multi-core (ARM big.LITTLE) architectures.

---

## 👥 Team Members

* **M. Ahmad Aniz** (25BCE1139) - *Lead Concurrency Architect & Systems Programmer*
* **G S Mithul Kruthik** (25BCE1560) - *Operating Systems Kernel & Scheduling Specialist*
* **Siva Kumar P** (25BCE5735) - *Mathematical Modeling, Visualization & Multimedia Lead*

---

## ⚡ Key Features

1. **Requirement R3: Real-Time CPU Core & Scheduling Monitor**:
   * Reads `/proc/self/task/[TID]/stat` (Field 39 `processor`) to inspect the physical hardware core running each thread without root permissions.
   * Classifies heterogeneous cores into energy-efficient **LITTLE** and performance **BIG** clusters using `/sys/devices/system/cpu/`.
   * Live 8-core visual grid in the UI dynamically highlighting active cores with thread tags (e.g., `🧵 T0 [TID 2381]`).
   * Linux Completely Fair Scheduler (CFS) priority selector: **Normal (Nice 0)**, **Background (Nice +10)**, and **Urgent (Nice -16)** via `Process.setThreadPriority`.

2. **Java & Kotlin Coexistence (Functions vs. Listeners)**:
   * **Java Engine** (`CpuBenchmarkEngine.java`): Uses `ExecutorService`, `Callable`, `Future`, and traditional Observer Listeners (`BenchmarkListener.java`).
   * **Kotlin Engine** (`BenchmarkRunner.kt`): Idiomatic functional concurrency returning immutable `BenchmarkResult` data classes.
   * **SAM Conversion**: Demonstrates how Kotlin seamlessly passes first-class lambda functions where Java single-method listeners are expected (`BenchmarkProgressListener.java`).

3. **Amdahl's Law Speedup Modeling**:
   * Exact mathematical formula: $$S(N) = \frac{1}{(1 - P) + \frac{P}{N}}$$
   * Empirically derives the Parallel Fraction ($P$) and Serial Bottleneck ($1 - P$).
   * Interactive slider (50% to 99%) projecting theoretical speedups up to 64 cores.
   * Visual chart overlay comparing measured execution times against Amdahl's theoretical curve.

4. **Concurrency Safety & Race Condition Analyzer**:
   * Measures lost updates and data loss percentages when multiple threads modify an unsynchronized counter (`count++`).
   * Contrasts against atomic hardware Compare-And-Swap (CAS) operations using `AtomicLong`.
   * Quantifies synchronization overhead percentage.

5. **In-App Multimedia Pedagogy**:
   * Custom 60 FPS vector animated video (`AnimatedVideoCanvasView.kt`) illustrating big.LITTLE core allocation, race conditions, Amdahl curves, and Looper thread boundaries.
   * 100% offline spoken voice narration using Android's native `TextToSpeech` engine (`TutorialAudioManager.kt`).
   * In-app Code & OS Architecture Study Guide.

---

## 🚀 Getting Started

### Prerequisites
* Android Studio (Ladybug, Meerkat, or newer)
* Android SDK (API Level 24 to 36)
* JDK 17 or 21

### Building and Running
1. Clone or extract this repository into your Android Studio projects directory:
   ```bash
   git clone <repository-url>
   ```
2. Open Android Studio and select **Open** -> Select the project root folder.
3. Allow Gradle to sync.
4. Select a connected device or emulator and click **Run (▶)**.

### Building APK via Terminal
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 Academic Project Report
The complete 9-page engineering report with architectural diagrams, mathematical derivations, and live screenshots is included in this repository:
* Markdown: [`PROJECT_REPORT.md`](PROJECT_REPORT.md)
* PDF: [`PROJECT_REPORT.pdf`](PROJECT_REPORT.pdf)
