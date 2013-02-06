package org.terasology.monitoring;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.terasology.math.Vector3i;
import org.terasology.monitoring.impl.ChunkEvent;
import org.terasology.world.chunks.Chunk;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class ChunkMonitor {

    private static final EventBus eventbus = new EventBus("ChunkMonitor");
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final LinkedList<Entry> chunks = new LinkedList<Entry>();
    
    private ChunkMonitor() {}

    public static EventBus getEventBus() {
        return eventbus;
    }
    
    public static void registerChunk(Chunk chunk) {
        Preconditions.checkNotNull(chunk, "The parameter 'chunk' must not be null");
        lock.writeLock().lock();
        try {
            chunks.add(new Entry(chunk));
        } finally {
            lock.writeLock().unlock();
        }
        eventbus.post(new ChunkEvent.Created(chunk));
    }
    
    protected static class Entry {
        
        public final Vector3i position;
        public final WeakReference<Chunk> ref;
        
        public Entry(Chunk chunk) {
            Preconditions.checkNotNull(chunk, "The parameter 'chunk' must not be null");
            this.position = chunk.getPos();
            this.ref = new WeakReference<Chunk>(chunk);
        }
    }
}
