/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.world;

import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;
import org.terasology.world.block.Block;
import org.terasology.world.block.management.BlockManager;
import org.terasology.world.chunks.Chunk;
import org.terasology.world.chunks.ChunkType;
import org.terasology.world.chunks.provider.ChunkProvider;
import org.terasology.world.liquid.LiquidData;

import com.google.common.base.Preconditions;

/**
 * This class is deprecated in favor of the classes in package org.terasology.world.views!
 * <p/>
 * The following classes will have to be refactored so they don't use ClassicWorldView anymore:<br/>
 *      #   src/main/java/org/terasology/rendering/primitives/ChunkTessellator.java<br/>
 *      #   src/main/java/org/terasology/rendering/world/ChunkUpdateManager.java<br/>
 *      #   src/main/java/org/terasology/rendering/world/WorldRenderer.java<br/>
 *      #   src/main/java/org/terasology/world/AbstractWorldProviderDecorator.java<br/>
 *      #   src/main/java/org/terasology/world/WorldProviderCore.java<br/>
 *      #   src/main/java/org/terasology/world/WorldProviderCoreImpl.java<br/>
 *      #   src/main/java/org/terasology/world/chunks/LocalChunkProvider.java<br/>
 *      #   src/main/java/org/terasology/world/generator/SecondPassChunkGenerator.java<br/>
 *      #   src/main/java/org/terasology/world/generator/core/ChunkGeneratorManager.java<br/>
 *      #   src/main/java/org/terasology/world/generator/core/ChunkGeneratorManagerImpl.java<br/>
 *      #   src/main/java/org/terasology/world/generator/core/ForestGenerator.java<br/>
 *      #   src/main/java/org/terasology/world/generator/tree/TreeGenerator.java<br/>
 *      #   src/main/java/org/terasology/world/generator/tree/TreeGeneratorCactus.java<br/>
 *      #   src/main/java/org/terasology/world/generator/tree/TreeGeneratorLSystem.java<br/>
 *      #   src/main/java/org/terasology/world/lighting/LightPropagator.java<br/>
 *      #   src/main/java/org/terasology/world/liquid/LiquidSimulator.java<br/>
 *      #   src/test/java/org/terasology/testUtil/WorldProviderCoreStub.java<br/>
 *      #   src/test/java/org/terasology/world/LightPropagationTest.java<br/>
 *      #   src/test/java/org/terasology/world/WorldViewTest.java<br/>
 *      #   src/test/java/org/terasology/world/liquid/LiquidSimulationTest.java<br/>
 * 
 * @author Immortius
 */
@Deprecated
public class ClassicWorldView {

    private Vector3i offset;
    private Region3i chunkRegion;
    private Region3i blockRegion;
    private Chunk[][] chunks;
    
    private ChunkType chunkType;

    public static ClassicWorldView createLocalView(Vector3i chunkPos, ChunkProvider chunkProvider) {
        final ChunkType chunkType = chunkProvider.getChunkType();
        final Region3i region = Region3i.createFromCenterExtents(chunkPos, chunkType.getChunkExtents(1));
        final Vector3i offset = new Vector3i(1, chunkType.fStackable, 1);
        return createWorldView(region, offset, chunkProvider); 
    }

    public static ClassicWorldView createSubviewAroundBlock(Vector3i blockPos, int extent, ChunkProvider chunkProvider) {
        final ChunkType chunkType = chunkProvider.getChunkType();
        final Region3i region = chunkType.getChunkRegionAroundBlockPos(blockPos, extent);
        final Vector3i offset = new Vector3i(-region.min().x, -region.min().y * chunkType.fStackable, -region.min().z);
        return createWorldView(region, offset, chunkProvider);
    }

    public static ClassicWorldView createSubviewAroundChunk(Vector3i chunkPos, ChunkProvider chunkProvider) {
        final ChunkType chunkType = chunkProvider.getChunkType();
        final Region3i region = Region3i.createFromCenterExtents(chunkPos, chunkType.getChunkExtents(1));
        final Vector3i offset = new Vector3i(-region.min().x, -region.min().y * chunkType.fStackable, -region.min().z);
        return createWorldView(region, offset, chunkProvider);
    }

    public static ClassicWorldView createWorldView(Region3i chunkRegion, Vector3i offset, ChunkProvider chunkProvider) {
        final Vector3i size = chunkRegion.size();
        final Vector3i min = chunkRegion.min();
        final Chunk[][] chunks = new Chunk[size.y][size.x * size.z];
        for (Vector3i pos : chunkRegion) {
            final Chunk chunk = chunkProvider.getChunk(pos);
            if (chunk == null)
                return null;
            final int row = (pos.y - min.y);
            final int col = (pos.x - min.x) + size.x * (pos.z - min.z);
            chunks[row][col] = chunk; 
        }
        return new ClassicWorldView(chunks, chunkRegion, offset, chunkProvider.getChunkType());
    }

    public ClassicWorldView(Chunk[][] chunks, Region3i chunkRegion, Vector3i offset, ChunkType chunkType) {
        this.chunks = Preconditions.checkNotNull(chunks, "The parameter 'chunks' must not be null");
        this.chunkRegion = Preconditions.checkNotNull(chunkRegion, "The parameter 'chunkRegion' must not be null");
        this.offset = Preconditions.checkNotNull(offset, "The parameter 'offset' must not be null");
        setChunkType(chunkType);
    }

    public Region3i getChunkRegion() {
        return chunkRegion;
    }

    public Block getBlock(float x, float y, float z) {
        return getBlock(TeraMath.floorToInt(x + 0.5f), TeraMath.floorToInt(y + 0.5f), TeraMath.floorToInt(z + 0.5f));
    }

    public Block getBlock(Vector3i pos) {
        return getBlock(pos.x, pos.y, pos.z);
    }

    // TODO: Review
    public Block getBlock(int blockX, int blockY, int blockZ) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) 
            return BlockManager.getInstance().getAir();
        return chunk.getBlock(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ));
    }

    public byte getSunlight(float x, float y, float z) {
        return getSunlight(TeraMath.floorToInt(x + 0.5f), TeraMath.floorToInt(y + 0.5f), TeraMath.floorToInt(z + 0.5f));
    }

    public byte getSunlight(Vector3i pos) {
        return getSunlight(pos.x, pos.y, pos.z);
    }

    public byte getLight(float x, float y, float z) {
        return getLight(TeraMath.floorToInt(x + 0.5f), TeraMath.floorToInt(y + 0.5f), TeraMath.floorToInt(z + 0.5f));
    }

    public byte getLight(Vector3i pos) {
        return getLight(pos.x, pos.y, pos.z);
    }

    public byte getSunlight(int blockX, int blockY, int blockZ) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) 
            return 0;
        return chunk.getSunlight(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ));
    }

    public byte getLight(int blockX, int blockY, int blockZ) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) 
            return 0;
        return chunk.getLight(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ));
    }

    public boolean setBlock(Vector3i pos, Block type, Block oldType) {
        return setBlock(pos.x, pos.y, pos.z, type, oldType);
    }

    public boolean setBlock(int blockX, int blockY, int blockZ, Block type, Block oldType) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) 
            return false;
        return chunk.setBlock(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ), type, oldType);
    }

    public LiquidData getLiquid(Vector3i pos) {
        return getLiquid(pos.x, pos.y, pos.z);
    }

    public LiquidData getLiquid(int blockX, int blockY, int blockZ) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) 
            return new LiquidData();
        return chunk.getLiquid(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ));
    }

    public boolean setLiquid(Vector3i pos, LiquidData newState, LiquidData oldState) {
        return setLiquid(pos.x, pos.y, pos.z, newState, oldState);
    }

    public boolean setLiquid(int blockX, int blockY, int blockZ, LiquidData newState, LiquidData oldState) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) 
            return false;
        return chunk.setLiquid(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ), newState, oldState);
    }

    public void setLight(Vector3i pos, byte light) {
        setLight(pos.x, pos.y, pos.z, light);
    }

    public void setSunlight(Vector3i pos, byte light) {
        setSunlight(pos.x, pos.y, pos.z, light);
    }

    public void setSunlight(int blockX, int blockY, int blockZ, byte sunlight) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) return;
        chunk.setSunlight(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ), sunlight);
    }

    public void setLight(int blockX, int blockY, int blockZ, byte light) {
        final Chunk chunk = getChunkFromBlockPos(blockX, blockY, blockZ);
        if (chunk == null) return;
        chunk.setLight(chunkType.calcBlockPosX(blockX), chunkType.calcBlockPosY(blockY), chunkType.calcBlockPosZ(blockZ), light);
    }

    public void setDirtyAround(Vector3i blockPos) {
        for (Vector3i pos : chunkType.getChunkRegionAroundBlockPos(blockPos, 1)) {
            getChunkFromChunkPos(pos).setDirty(true);
        }
    }

    public void setDirtyAround(Region3i blockRegion) {
        for (Vector3i pos : chunkType.getChunkRegionAroundBlockRegion(blockRegion, 1)) { 
            getChunkFromChunkPos(pos).setDirty(true);
        }
    }

    public void lock() {
        for (Chunk[] row : chunks)
            for (Chunk chunk : row)
                chunk.lock();
    }

    public void unlock() {
        for (Chunk[] row : chunks)
            for (Chunk chunk : row)
                chunk.unlock();
    }

    public boolean isValidView() {
        for (Chunk[] row : chunks)
            for (Chunk chunk : row)
                if (chunk.isDisposed())
                    return false;
        return true;
    }

    public Chunk getChunkFromBlockPos(int blockX, int blockY, int blockZ) {
        if (blockRegion.encompasses(blockX, blockY, blockZ)) {
            final int row = chunkType.calcChunkPosY(blockY) + offset.y;
            final int col = chunkType.calcChunkPosX(blockX) + offset.x + chunkRegion.size().x * (chunkType.calcChunkPosZ(blockZ) + offset.z);
            return chunks[row][col];
        }
        return null;
    }
    
    public Chunk getChunkFromBlockPos(Vector3i block) {
        return getChunkFromBlockPos(block.x, block.y, block.z);
    }
    
    public Chunk getChunkFromChunkPos(int chunkX, int chunkY, int chunkZ) {
        if (chunkRegion.encompasses(chunkX, chunkY, chunkZ)) {
            final int row = chunkY + offset.y;
            final int col = chunkX + offset.x + chunkRegion.size().x * (chunkZ + offset.z);
            return chunks[row][col];
        }
        return null;
    }
    
    public Chunk getChunkFromChunkPos(Vector3i chunk) {
        return getChunkFromChunkPos(chunk.x, chunk.y, chunk.z);
    }
    
    public void setChunkType(ChunkType chunkType) {
        Preconditions.checkNotNull(chunkType, "The parameter 'chunkType' must not be null");
        if (this.chunkType != chunkType) {
            this.chunkType = chunkType;
            final Vector3i blockMin = new Vector3i();
            blockMin.sub(offset);
            blockMin.mult(chunkType.sizeX, chunkType.sizeY * chunkType.fStackable, chunkType.sizeZ);
            final Vector3i blockSize = chunkRegion.size();
            blockSize.mult(chunkType.sizeX, chunkType.sizeY, chunkType.sizeZ);
            this.blockRegion = Region3i.createFromMinAndSize(blockMin, blockSize);
        }
    }

    public Vector3i toWorldPos(Vector3i localPos) {
        final int worldPosX = localPos.x + (offset.x + chunkRegion.min().x) * chunkType.sizeX;
        final int worldPosY = chunkType.isStackable ? localPos.y + (offset.y + chunkRegion.min().y) * chunkType.sizeY : localPos.y;
        final int worldPosZ = localPos.z + (offset.z + chunkRegion.min().z) * chunkType.sizeZ;
        return new Vector3i(worldPosX, worldPosY, worldPosZ);
    }
}
