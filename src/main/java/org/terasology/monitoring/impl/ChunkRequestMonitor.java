package org.terasology.monitoring.impl;

import org.terasology.monitoring.ThreadMonitor.SingleThreadMonitor;

public class ChunkRequestMonitor extends SingleThreadMonitor {

    public ChunkRequestMonitor(Thread thread) {
        super("LocalChunkProvider.ChunkRequests", thread, "Review", "Produce");
    }

}
