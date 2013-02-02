package org.terasology.monitoring.impl;

import org.terasology.monitoring.ThreadMonitor.SingleThreadMonitor;

public class ChunkStoreMonitor extends SingleThreadMonitor {

    public ChunkStoreMonitor(String storeType, Thread thread) {
        super("ChunkStore." + storeType, thread, "Chunks");
    }

}
