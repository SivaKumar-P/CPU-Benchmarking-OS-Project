package com.aniz.cpubenchmark;

import android.os.Process;

/**
 * Operating System Thread Scheduling Policies and Niceness priorities.
 * Maps high-level benchmark choices to Linux kernel nice values.
 */
public enum SchedulingPolicy {
    NORMAL("Normal (CFS Nice 0)", Process.THREAD_PRIORITY_DEFAULT),
    BACKGROUND("Background (Nice +10)", Process.THREAD_PRIORITY_BACKGROUND),
    URGENT("Urgent Display (Nice -16)", Process.THREAD_PRIORITY_URGENT_DISPLAY);

    private final String label;
    private final int nicePriority;

    SchedulingPolicy(String label, int nicePriority) {
        this.label = label;
        this.nicePriority = nicePriority;
    }

    public String getLabel() {
        return label;
    }

    public int getNicePriority() {
        return nicePriority;
    }
}
