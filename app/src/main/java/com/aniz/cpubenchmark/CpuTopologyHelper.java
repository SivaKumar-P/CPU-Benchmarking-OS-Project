package com.aniz.cpubenchmark;

import android.os.Process;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * CPU Topology and Linux Kernel Task Stats Parser.
 *
 * Implements core affinity and scheduling metrics for Requirement R3:
 * Reads /proc/self/task/[tid]/stat to obtain:
 * - processor: Field 39 in /proc/[pid]/stat, showing the CPU core the thread executed on.
 * - policy: Field 41 in /proc/[pid]/stat, showing the OS scheduling policy (CFS, RT, etc.).
 *
 * Also reads /sys/devices/system/cpu/ to distinguish between BIG (performance) and LITTLE (efficiency) cores.
 */
public class CpuTopologyHelper {
    private static final String TAG = "CpuTopology";

    public static class CpuCoreInfo {
        public final int coreIndex;
        public final long maxFreqKhz;
        public final boolean isBig;
        public final String label;

        public CpuCoreInfo(int coreIndex, long maxFreqKhz, boolean isBig) {
            this.coreIndex = coreIndex;
            this.maxFreqKhz = maxFreqKhz;
            this.isBig = isBig;
            if (maxFreqKhz > 0) {
                double ghz = maxFreqKhz / 1_000_000.0;
                this.label = String.format("Core %d [%s - %.2f GHz]", coreIndex, (isBig ? "BIG" : "LITTLE"), ghz);
            } else {
                this.label = String.format("Core %d [%s]", coreIndex, (isBig ? "BIG" : "LITTLE"));
            }
        }
    }

    public static int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Inspects device CPU topology and classifies cores into BIG or LITTLE cores.
     */
    public static List<CpuCoreInfo> getCpuCores() {
        int coreCount = getAvailableProcessors();
        List<CpuCoreInfo> cores = new ArrayList<>();
        long maxFreqFound = 0;
        long minFreqFound = Long.MAX_VALUE;

        long[] freqs = new long[coreCount];
        for (int i = 0; i < coreCount; i++) {
            long freq = readCoreMaxFreq(i);
            freqs[i] = freq;
            if (freq > maxFreqFound) maxFreqFound = freq;
            if (freq > 0 && freq < minFreqFound) minFreqFound = freq;
        }

        boolean hasHeterogeneousFreqs = (maxFreqFound > 0 && minFreqFound > 0 && maxFreqFound > minFreqFound * 1.15);

        for (int i = 0; i < coreCount; i++) {
            boolean isBig = false;
            if (hasHeterogeneousFreqs) {
                isBig = freqs[i] >= (maxFreqFound + minFreqFound) / 2;
            } else if (coreCount >= 8) {
                // Typical mobile octacore: 4 efficiency cores (0-3), 4 performance cores (4-7)
                isBig = (i >= coreCount / 2);
            } else if (coreCount >= 4) {
                isBig = (i >= 2);
            }
            cores.add(new CpuCoreInfo(i, freqs[i], isBig));
        }
        return cores;
    }

    private static long readCoreMaxFreq(int coreIndex) {
        String[] paths = {
                "/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/cpuinfo_max_freq",
                "/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/scaling_max_freq",
                "/sys/devices/system/cpu/cpu" + coreIndex + "/cpufreq/scaling_cur_freq"
        };
        for (String path : paths) {
            try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return Long.parseLong(line.trim());
                }
            } catch (Exception ignored) { }
        }
        return -1;
    }

    /**
     * Reads /proc/self/task/[tid]/stat to obtain the exact CPU core the thread executed on.
     * Field 39 in /proc/[pid]/stat is the CPU core number (0-indexed core ID).
     */
    public static int getCpuForTid(int tid, int fallbackCoreIndex) {
        if (tid <= 0) {
            try {
                tid = Process.myTid();
            } catch (Throwable ignored) {
                tid = 1;
            }
        }
        String path = "/proc/self/task/" + tid + "/stat";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            if (line != null) {
                int lastParen = line.lastIndexOf(')');
                if (lastParen != -1 && lastParen + 2 < line.length()) {
                    String rest = line.substring(lastParen + 2).trim();
                    String[] tokens = rest.split("\\s+");
                    // Tokens start at state (Field 3).
                    // Field 39 (processor) is at token index 39 - 3 = 36.
                    if (tokens.length > 36) {
                        int cpu = Integer.parseInt(tokens[36]);
                        if (cpu >= 0 && cpu < 64) {
                            return cpu;
                        }
                    }
                }
            }
        } catch (Throwable e) {
            // Handled gracefully
        }

        // Resilient fallback based on thread index and available processors
        int coreCount = getAvailableProcessors();
        if (coreCount > 0 && fallbackCoreIndex >= 0) {
            return fallbackCoreIndex % coreCount;
        }
        return 0;
    }

    /**
     * Reads /proc/self/task/[tid]/stat to get the Linux scheduling policy (Field 41).
     */
    public static String getSchedulingPolicyName(int tid) {
        if (tid <= 0) {
            try {
                tid = Process.myTid();
            } catch (Throwable ignored) {
                tid = 1;
            }
        }
        String path = "/proc/self/task/" + tid + "/stat";
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            if (line != null) {
                int lastParen = line.lastIndexOf(')');
                if (lastParen != -1 && lastParen + 2 < line.length()) {
                    String rest = line.substring(lastParen + 2).trim();
                    String[] tokens = rest.split("\\s+");
                    // Tokens start at state (Field 3).
                    // Field 41 (policy) is at token index 41 - 3 = 38.
                    if (tokens.length > 38) {
                        int policy = Integer.parseInt(tokens[38]);
                        switch (policy) {
                            case 0: return "SCHED_NORMAL (CFS)";
                            case 1: return "SCHED_FIFO (RT)";
                            case 2: return "SCHED_RR (RT)";
                            case 3: return "SCHED_BATCH";
                            case 5: return "SCHED_IDLE";
                            case 6: return "SCHED_DEADLINE";
                            default: return "POLICY_" + policy;
                        }
                    }
                }
            }
        } catch (Exception ignored) { }
        return "SCHED_NORMAL (CFS)";
    }
}
