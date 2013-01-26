package org.terasology.world.chunks;

import java.util.Set;

import org.terasology.math.Vector3i;
import org.terasology.world.chunks.blockdata.TeraArrayIterator;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**
 * This is an experimental implementation of a simple way to retrieve some statistics about a chunk.
 * It will be subject to change.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public class ChunkStatistics {
    
    private final Vector3i chunk;
    private final Multiset<Integer> blocks = HashMultiset.create();
    
    private void computeBlockCounts(final Chunk chunk) {
        final TeraArrayIterator it = chunk.getBlockData().iterator();
        int lastBlock = it.value(), count = 1;
        it.advance();
        while (it.hasNext()) {
            final int block = it.value();
            if (lastBlock == block) {
                count++;
            } else {
                blocks.add(lastBlock, count);
                lastBlock = block;
                count = 1;
            }
            it.advance();
        }
        blocks.add(lastBlock, count);
    }
    
    public ChunkStatistics(Chunk chunk) {
        Preconditions.checkNotNull(chunk, "The parameter 'chunk' must not be null");
        this.chunk = chunk.getPos();
        computeBlockCounts(chunk);
    }
    
    public final Vector3i getChunk() {
        return new Vector3i(chunk);
    }
    
    public final Set<Integer> getBlocks() {
        return blocks.elementSet();
    }
    
    public final int getBlockOccurrences(int id) {
        return blocks.count(id);
    }
}