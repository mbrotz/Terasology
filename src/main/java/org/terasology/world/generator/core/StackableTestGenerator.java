package org.terasology.world.generator.core;

import java.util.Map;

import org.terasology.math.Vector3i;
import org.terasology.world.WorldBiomeProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.management.BlockManager;
import org.terasology.world.chunks.Chunk;
import org.terasology.world.chunks.blockdata.TeraArray;
import org.terasology.world.generator.ChunkGenerator;

public class StackableTestGenerator implements ChunkGenerator {

    private Block air = BlockManager.getInstance().getAir();
    private Block stone = BlockManager.getInstance().getBlock("engine:Stone");
    private Block dirt = BlockManager.getInstance().getBlock("engine:Dirt");
    private Block lava = BlockManager.getInstance().getBlock("engine:Lava");
    
    @Override
    public void setWorldSeed(String seed) {}

    @Override
    public void setWorldBiomeProvider(WorldBiomeProvider biomeProvider) {}

    @Override
    public Map<String, String> getInitParameters() {
        return null;
    }

    @Override
    public void setInitParameters(Map<String, String> initParameters) {}

    @Override
    public void generateChunk(Chunk chunk) {
        
        final Vector3i chunkPos = chunk.getPos();
        final int sizeX = chunk.getChunkSizeX(), sizeY = chunk.getChunkSizeY(), sizeZ = chunk.getChunkSizeZ();
        final TeraArray blocks = chunk.getBlockData(), sunlight = chunk.getSunlightData(), light = chunk.getLightData();
        
        final byte fill;
        if (chunkPos.y >= 0) 
            fill = air.getId();
        else if (chunkPos.y > -5) 
            fill = dirt.getId();
        else if (chunkPos.y > -15) 
            fill = stone.getId();
        else
            fill = lava.getId();
        
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    blocks.set(x, y, z, fill);
                    sunlight.set(x, y, z, Chunk.MAX_LIGHT);
                    light.set(x, y, z, Chunk.MAX_LIGHT);
                }
            }
        }
//
//        if (chunkPos.y != -1) {
//            final byte id = stone.getId();
//            final int maxX = sizeX-1, maxY = sizeY-1, maxZ = sizeZ-1;
//            blocks.set(0, 0, 0, id);
//            blocks.set(0, maxY, 0, id);
//            blocks.set(0, maxY, maxZ, id);
//            blocks.set(0, 0, maxZ, id);
//            blocks.set(maxX, 0, 0, id);
//            blocks.set(maxX, maxY, 0, id);
//            blocks.set(maxX, maxY, maxZ, id);
//            blocks.set(maxX, 0, maxZ, id);
//        }
   }
}
