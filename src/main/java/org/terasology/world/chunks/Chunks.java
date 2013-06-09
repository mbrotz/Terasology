package org.terasology.world.chunks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.config.Config;
import org.terasology.game.CoreRegistry;
import org.terasology.protobuf.ChunksProtobuf;
import org.terasology.world.chunks.blockdata.TeraArrays;
import org.terasology.world.chunks.blockdata.TeraDenseArray8Bit;

/**
 * The class Chunks holds information about the datatypes the engine uses to store per block data.
 * <p/>
 * Serialization and deserialization of Chunks into/from protobuf messages is supported through the methods
 * {@code Chunks.encode(Chunk)} and {@code Chunks.decode(ChunksProtobuf.Chunk)}.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public final class Chunks {

    private static final Logger logger = LoggerFactory.getLogger(Chunks.class);
    private static final Chunks instance = new Chunks();
    
    private final TeraArrays.Entry blockDataEntry;
    private final TeraArrays.Entry sunlightDataEntry;
    private final TeraArrays.Entry lightDataEntry;
    private final TeraArrays.Entry extraDataEntry;

    private final Chunk.ProtobufHandler handler;
    
    private Chunks() {
        final Config config = CoreRegistry.get(Config.class);
        final TeraArrays t = TeraArrays.getInstance();
        if (config == null) {
            logger.warn("Unable to obtain global configuration object. Using 8-bit dense arrays for all per block data.");
            blockDataEntry = t.getEntry(TeraDenseArray8Bit.class);
            sunlightDataEntry = t.getEntry(TeraDenseArray8Bit.class);
            lightDataEntry = t.getEntry(TeraDenseArray8Bit.class);
            extraDataEntry = t.getEntry(TeraDenseArray8Bit.class);
        } else {
            blockDataEntry = t.getEntry(config.getAdvanced().getBlockDataClassName());
            sunlightDataEntry = t.getEntry(config.getAdvanced().getSunlightDataClassName());
            lightDataEntry = t.getEntry(config.getAdvanced().getLightDataClassName());
            extraDataEntry = t.getEntry(config.getAdvanced().getExtraDataClassName());
        }
        
        handler = new Chunk.ProtobufHandler();
    }

    public final TeraArrays.Entry getBlockDataEntry() {
        return blockDataEntry;
    }

    public final TeraArrays.Entry getSunlightDataEntry() {
        return sunlightDataEntry;
    }

    public final TeraArrays.Entry getLightDataEntry() {
        return lightDataEntry;
    }

    public final TeraArrays.Entry getExtraDataEntry() {
        return extraDataEntry;
    }
    
    public final ChunksProtobuf.Chunk encode(Chunk chunk) {
        return handler.encode(chunk);
    }
    
    public final Chunk decode(ChunksProtobuf.Chunk message) {
        return handler.decode(message);
    }
    
    public static final Chunks getInstance() {
        return instance;
    }
}
