package org.terasology.monitoring.impl;

import org.terasology.monitoring.ThreadMonitor.SingleThreadMonitor;

public class ChunkProcessingMonitor extends SingleThreadMonitor {

    public ChunkProcessingMonitor(Thread thread) {
        super("LocalChunkProvider.ChunkProcessing", thread, "Tasks");
    }

}
