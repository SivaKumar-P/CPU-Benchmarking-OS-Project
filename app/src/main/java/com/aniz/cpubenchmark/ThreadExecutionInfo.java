package com.aniz.cpubenchmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates thread execution metrics and hardware core affinity.
 * Used to satisfy Requirement R3: displaying which CPU core each thread executes on.
 */
public class ThreadExecutionInfo {
    private final int threadIndex;
    private final int tid;
    private int cpuCore;
    private final String coreType; // "BIG", "LITTLE", or "CORE"
    private final int priority;   // Linux nice value
    private final String schedulingPolicy;
    private long workUnits;
    private long elapsedMs;
    private final List<Integer> coreHistory = new ArrayList<>();

    public ThreadExecutionInfo(int threadIndex, int tid, int cpuCore, String coreType, int priority, String schedulingPolicy) {
        this.threadIndex = threadIndex;
        this.tid = tid;
        this.cpuCore = cpuCore;
        this.coreType = coreType;
        this.priority = priority;
        this.schedulingPolicy = schedulingPolicy;
        if (cpuCore >= 0) {
            this.coreHistory.add(cpuCore);
        }
    }

    public int getThreadIndex() {
        return threadIndex;
    }

    public int getTid() {
        return tid;
    }

    public int getCpuCore() {
        return cpuCore;
    }

    public void setCpuCore(int core) {
        this.cpuCore = core;
        if (core >= 0 && !coreHistory.contains(core)) {
            coreHistory.add(core);
        }
    }

    public String getCoreType() {
        return coreType;
    }

    public int getPriority() {
        return priority;
    }

    public String getSchedulingPolicy() {
        return schedulingPolicy;
    }

    public long getWorkUnits() {
        return workUnits;
    }

    public void setWorkUnits(long workUnits) {
        this.workUnits = workUnits;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public List<Integer> getCoreHistory() {
        return coreHistory;
    }

    @Override
    public String toString() {
        return "Thread #" + threadIndex + " [TID: " + tid + "] -> Core " +
                (cpuCore >= 0 ? cpuCore : "N/A") + " (" + coreType + ", Nice: " + priority +
                ", Policy: " + schedulingPolicy + ")";
    }
}
