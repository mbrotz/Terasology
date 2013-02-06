package org.terasology.monitoring.impl;

import java.util.Date;

import org.terasology.math.Vector3i;
import org.terasology.world.chunks.Chunk;
import org.terasology.world.chunks.ChunkState;

import com.google.common.base.Preconditions;

public abstract class ChunkEvent {

    public final Vector3i position;
    public final Date time;
    
    public ChunkEvent(Chunk chunk) {
        Preconditions.checkNotNull(chunk, "The parameter 'chunk' must not be null");
        this.position = chunk.getPos();
        this.time = new Date();
    }

    public static class Created extends ChunkEvent {
        public Created(Chunk chunk) {
            super(chunk);
        }
    }
    
    public static class Disposed extends ChunkEvent {
        public Disposed(Chunk chunk) {
            super(chunk);
        }
    }
    
    public static class StateChanged extends ChunkEvent {
        
        public final ChunkState oldState, newState;
        
        public StateChanged(Chunk chunk, ChunkState oldState) {
            super(chunk);
            this.oldState = oldState;
            this.newState = chunk.getChunkState();
        }
    }
    
    public static class Deflated extends ChunkEvent {
        
        public final int oldSize, newSize;
        
        public Deflated(Chunk chunk, int oldSize, int newSize) {
            super(chunk);
            this.oldSize = oldSize;
            this.newSize = newSize;
        }
    }
}
