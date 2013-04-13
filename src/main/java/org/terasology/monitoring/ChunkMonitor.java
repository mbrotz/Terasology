package org.terasology.monitoring;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.terasology.monitoring.impl.ChunkEvent;
import org.terasology.world.chunks.Chunk;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class ChunkMonitor {

    private static final EventBus eventbus = new EventBus("ChunkMonitor");
    private static final ReentrantLock lock = new ReentrantLock();
    private static final LinkedList<WeakChunk> chunks = new LinkedList<WeakChunk>();
    
    private ChunkMonitor() {}

    public static EventBus getEventBus() {
        return eventbus;
    }
    
    public static void registerChunk(Chunk chunk) {
        Preconditions.checkNotNull(chunk, "The parameter 'chunk' must not be null");
        final WeakChunk w = new WeakChunk(chunk);
        lock.lock();
        try {
            chunks.add(w);
        } finally {
            lock.unlock();
        }
        eventbus.post(new ChunkEvent.Created(w));
    }
    
    public static void getChunks(List<Chunk> output) {
        lock.lock();
        try {
            final Iterator<WeakChunk> it = chunks.iterator();
            while (it.hasNext()) {
                final WeakChunk e = it.next();
                final Chunk c = e.ref.get();
                if (c != null) {
                    output.add(c);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public static void getWeakChunks(List<WeakChunk> output) {
        lock.lock();
        try {
            final Iterator<WeakChunk> it = chunks.iterator();
            while (it.hasNext()) {
                final WeakChunk e = it.next();
                final Chunk c = e.ref.get();
                if (c != null) {
                    output.add(e);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    
    public static int purgeDeadChunks() {
        lock.lock();
        try {
            int result = 0;
            final Iterator<WeakChunk> it = chunks.iterator();
            while (it.hasNext()) {
                final WeakChunk e = it.next();
                final Chunk c = e.ref.get();
                if (c == null) {
                    it.remove();
                    ++result;
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
