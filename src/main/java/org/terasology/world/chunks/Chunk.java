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
package org.terasology.world.chunks;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.DecimalFormat;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.vecmath.Vector3f;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.config.AdvancedConfig;
import org.terasology.game.CoreRegistry;
import org.terasology.logic.manager.Config;
import org.terasology.math.AABB;
import org.terasology.math.Vector3i;
import org.terasology.monitoring.ChunkMonitor;
import org.terasology.monitoring.impl.ChunkEvent;
import org.terasology.protobuf.ChunksProtobuf;
import org.terasology.rendering.primitives.ChunkMesh;
import org.terasology.world.block.Block;
import org.terasology.world.block.management.BlockManager;
import org.terasology.world.chunks.blockdata.TeraArray;
import org.terasology.world.chunks.blockdata.TeraArrays;
import org.terasology.world.chunks.deflate.TeraStandardDeflator;
import org.terasology.world.chunks.deflate.TeraDeflator;
import org.terasology.world.liquid.LiquidData;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * Chunks are the basic components of the world. Each chunk contains a fixed amount of blocks
 * determined by its dimensions. They are used to manage the world efficiently and
 * to reduce the batch count within the render loop.
 * <p/>
 * Chunks are tessellated on creation and saved to vertex arrays. From those VBOs are generated
 * which are then used for the actual rendering process.
 *
 * @author Benjamin Glatzel <benjamin.glatzel@me.com>
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 */
public class Chunk implements Externalizable {
    protected static final Logger logger = LoggerFactory.getLogger(Chunk.class);
    
    public static final long serialVersionUID = 79881925217704826L;
    
    /* PUBLIC CONSTANT VALUES */
    public static final int VERTICAL_SEGMENTS = Config.getInstance().getVerticalChunkMeshSegments();
    public static final byte MAX_LIGHT = 0x0f;
    public static final byte MAX_LIQUID_DEPTH = 0x07;

    private final ReentrantReadWriteLock accessLock = new ReentrantReadWriteLock();
    
    private final ChunkType chunkType;
    private ChunkState chunkState = ChunkState.ADJACENCY_GENERATION_PENDING;    
    private final Vector3i pos = new Vector3i();

    private TeraArray blockData;
    private TeraArray sunlightData;
    private TeraArray lightData;
    private TeraArray extraData;

    private boolean dirty;
    private boolean animated;
    private AABB aabb;

    // Rendering
    private ChunkMesh[] mesh;
    private ChunkMesh[] pendingMesh;
    private AABB[] subMeshAABB = null;

    private boolean disposed = false;

    /**
     * This constructor is for compatibility only and will be removed later.
     */
    @Deprecated
    public Chunk() {
        this(ChunkType.Classic);
    }
    
    public Chunk(ChunkType chunkType) {
        this.chunkType = Preconditions.checkNotNull(chunkType, "The parameter 'chunkType' must not be null");
        final Chunks c = Chunks.getInstance();
        blockData = c.getBlockDataEntry().factory.create(getChunkSizeX(), getChunkSizeY(), getChunkSizeZ());
        sunlightData = c.getSunlightDataEntry().factory.create(getChunkSizeX(), getChunkSizeY(), getChunkSizeZ());
        lightData = c.getLightDataEntry().factory.create(getChunkSizeX(), getChunkSizeY(), getChunkSizeZ());
        extraData = c.getExtraDataEntry().factory.create(getChunkSizeX(), getChunkSizeY(), getChunkSizeZ());
        dirty = true;
        ChunkMonitor.registerChunk(this);
    }

    public Chunk(ChunkType chunkType, int x, int y, int z) {
        this(chunkType);
        pos.x = x;
        pos.y = y;
        pos.z = z;
    }

    public Chunk(ChunkType chunkType, Vector3i pos) {
        this(chunkType, pos.x, pos.y, pos.z);
    }

    public Chunk(Chunk other) {
        chunkType = other.chunkType;
        pos.set(other.pos);
        blockData = other.blockData.copy();
        sunlightData = other.sunlightData.copy();
        lightData = other.lightData.copy();
        extraData = other.extraData.copy();
        chunkState = other.chunkState;
        dirty = true;
        ChunkMonitor.registerChunk(this);
    }
    
    public Chunk(ChunkType chunkType, Vector3i pos, ChunkState chunkState, TeraArray blocks, TeraArray sunlight, TeraArray light, TeraArray liquid) {
        this.chunkType = Preconditions.checkNotNull(chunkType);
        this.pos.set(Preconditions.checkNotNull(pos));
        this.blockData = Preconditions.checkNotNull(blocks);
        this.sunlightData = Preconditions.checkNotNull(sunlight);
        this.lightData = Preconditions.checkNotNull(light);
        this.extraData = Preconditions.checkNotNull(liquid);
        this.chunkState = Preconditions.checkNotNull(chunkState);
        dirty = true;
        ChunkMonitor.registerChunk(this);
    }
    
    /**
     * ProtobufHandler implements support for encoding/decoding chunks into/from protobuf messages.
     * 
     * @author Manuel Brotz <manu.brotz@gmx.ch>
     * @todo Add support for chunk data extensions.
     *
     */
    public static class ProtobufHandler implements org.terasology.io.ProtobufHandler<Chunk, ChunksProtobuf.Chunk> {

        @Override
        public ChunksProtobuf.Chunk encode(Chunk chunk) {
            Preconditions.checkNotNull(chunk, "The parameter 'chunk' must not be null");
            final TeraArrays t = TeraArrays.getInstance();
            final ChunksProtobuf.Chunk.Builder b = ChunksProtobuf.Chunk.newBuilder()
                    .setX(chunk.pos.x).setY(chunk.pos.y).setZ(chunk.pos.z)
                    .setType(chunk.getChunkType().id)
                    .setState(chunk.getChunkState().id)
                    .setBlockData(t.encode(chunk.blockData))
                    .setSunlightData(t.encode(chunk.sunlightData))
                    .setLightData(t.encode(chunk.lightData))
                    .setExtraData(t.encode(chunk.extraData));
            return b.build(); 
        }

        @Override
        public Chunk decode(ChunksProtobuf.Chunk message) {
            Preconditions.checkNotNull(message, "The parameter 'message' must not be null");
            if (!message.hasX())
                throw new IllegalArgumentException("Illformed protobuf message. Missing x coordinate.");
            if (!message.hasY())
                throw new IllegalArgumentException("Illformed protobuf message. Missing y coordinate.");
            if (!message.hasZ())
                throw new IllegalArgumentException("Illformed protobuf message. Missing z coordinate.");
            final Vector3i pos = new Vector3i(message.getX(), message.getY(), message.getZ());
            if (!message.hasState())
                throw new IllegalArgumentException("Illformed protobuf message. Missing chunk state.");
            final ChunkState state = ChunkState.getStateById(message.getState());
            if (state == null)
                throw new IllegalArgumentException("Illformed protobuf message. Unknown chunk state: " + message.getState());
            if (!message.hasType())
                throw new IllegalArgumentException("Illformed protobuf message. Missing chunk type.");
            final ChunkType chunkType = ChunkType.getTypeById(message.getType());
            if (chunkType == null) 
                throw new IllegalArgumentException("Illformed protobuf message. Unknown chunk type: " + message.getType());
            if (!message.hasBlockData())
                throw new IllegalArgumentException("Illformed protobuf message. Missing block data.");
            if (!message.hasSunlightData())
                throw new IllegalArgumentException("Illformed protobuf message. Missing sunlight data.");
            if (!message.hasLightData())
                throw new IllegalArgumentException("Illformed protobuf message. Missing light data.");
            if (!message.hasExtraData())
                throw new IllegalArgumentException("Illformed protobuf message. Missing extra data.");
            final TeraArrays t = TeraArrays.getInstance();
            final TeraArray blockData = t.decode(message.getBlockData());
            final TeraArray sunlightData = t.decode(message.getSunlightData());
            final TeraArray lightData = t.decode(message.getLightData());
            final TeraArray extraData = t.decode(message.getExtraData());
            return new Chunk(chunkType, pos, state, blockData, sunlightData, lightData, extraData);
        }
    }
    
    public ReentrantReadWriteLock getLock() {
        return accessLock;
    }

    @Deprecated
    public void lock() {
        accessLock.writeLock().lock();
    }

    @Deprecated
    public void unlock() {
        accessLock.writeLock().unlock();
    }

    @Deprecated
    public boolean isLocked() {
        return accessLock.isWriteLocked();
    }

    public ChunkType getChunkType() {
        return chunkType;
    }
    
    public Vector3i getPos() {
        return new Vector3i(pos);
    }

    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < getChunkSizeX() && y < getChunkSizeY() && z < getChunkSizeZ();
    }

    public ChunkState getChunkState() {
        return chunkState;
    }

    public void setChunkState(ChunkState chunkState) {
        Preconditions.checkNotNull(chunkState);
        if (this.chunkState != chunkState) {
            final ChunkState old = this.chunkState;
            this.chunkState = chunkState;
            ChunkMonitor.getEventBus().post(new ChunkEvent.StateChanged(this, old));
        }
    }
    
    public ChunkStatistics getStatistics(final boolean accurate) {
        if (accurate) lock();
        try {
            return new ChunkStatistics(this);
        } finally {
            if (accurate) unlock();
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        lock();
        try {
            this.dirty = dirty;
        } finally {
            unlock();
        }
    }
    
    public TeraArray getBlockData() {
        return blockData;
    }
    
    public TeraArray getSunlightData() {
        return sunlightData;
    }
    
    public TeraArray getLightData() {
        return lightData;
    }
    
    public TeraArray getExtraData() {
        return extraData;
    }

    public int getEstimatedMemoryConsumptionInBytes() {
        return blockData.getEstimatedMemoryConsumptionInBytes() + sunlightData.getEstimatedMemoryConsumptionInBytes() + lightData.getEstimatedMemoryConsumptionInBytes() + extraData.getEstimatedMemoryConsumptionInBytes();
    }
    
    @Deprecated
    public Block getBlock(Vector3i pos) {
        return BlockManager.getInstance().getBlock((byte)blockData.get(pos.x, pos.y, pos.z));
    }

    @Deprecated
    public Block getBlock(int x, int y, int z) {
        return BlockManager.getInstance().getBlock((byte)blockData.get(x, y, z));
    }

    @Deprecated
    public boolean setBlock(int x, int y, int z, Block block) {
        int oldValue = blockData.set(x, y, z, block.getId());
        if (oldValue != block.getId()) {
            if (!block.isLiquid()) {
                setLiquid(x, y, z, new LiquidData());
            }
            return true;
        }
        return false;
    }

    @Deprecated
    public boolean setBlock(int x, int y, int z, Block newBlock, Block oldBlock) {
        if (newBlock != oldBlock) {
            if (blockData.set(x, y, z, newBlock.getId(), oldBlock.getId())) {
                if (!newBlock.isLiquid()) {
                    setLiquid(x, y, z, new LiquidData());
                }
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public boolean setBlock(Vector3i pos, Block block) {
        return setBlock(pos.x, pos.y, pos.z, block);
    }

    @Deprecated
    public boolean setBlock(Vector3i pos, Block block, Block oldBlock) {
        return setBlock(pos.x, pos.y, pos.z, block, oldBlock);
    }

    @Deprecated
    public byte getSunlight(Vector3i pos) {
        return getSunlight(pos.x, pos.y, pos.z);
    }

    @Deprecated
    public byte getSunlight(int x, int y, int z) {
        return (byte) sunlightData.get(x, y, z);
    }

    @Deprecated
    public boolean setSunlight(Vector3i pos, byte amount) {
        return setSunlight(pos.x, pos.y, pos.z, amount);
    }

    @Deprecated
    public boolean setSunlight(int x, int y, int z, byte amount) {
        Preconditions.checkArgument(amount >= 0 && amount <= 15);
        return sunlightData.set(x, y, z, amount) != amount;
    }

    @Deprecated
    public byte getLight(Vector3i pos) {
        return getLight(pos.x, pos.y, pos.z);
    }

    @Deprecated
    public byte getLight(int x, int y, int z) {
        return (byte) lightData.get(x, y, z);
    }

    @Deprecated
    public boolean setLight(Vector3i pos, byte amount) {
        return setLight(pos.x, pos.y, pos.z, amount);
    }

    @Deprecated
    public boolean setLight(int x, int y, int z, byte amount) {
        Preconditions.checkArgument(amount >= 0 && amount <= 15);
        return lightData.set(x, y, z, amount) != amount;
    }

    @Deprecated
    public boolean setLiquid(Vector3i pos, LiquidData newState, LiquidData oldState) {
        return setLiquid(pos.x, pos.y, pos.z, newState, oldState);
    }

    @Deprecated
    public boolean setLiquid(int x, int y, int z, LiquidData newState, LiquidData oldState) {
        byte expected = oldState.toByte();
        byte newValue = newState.toByte();
        return extraData.set(x, y, z, newValue, expected);
    }

    @Deprecated
    public void setLiquid(int x, int y, int z, LiquidData newState) {
        byte newValue = newState.toByte();
        extraData.set(x, y, z, newValue);
    }

    @Deprecated
    public LiquidData getLiquid(Vector3i pos) {
        return getLiquid(pos.x, pos.y, pos.z);
    }

    @Deprecated
    public LiquidData getLiquid(int x, int y, int z) {
        return new LiquidData((byte) extraData.get(x, y, z));
    }

    public Vector3i getChunkWorldPos() {
        return new Vector3i(getChunkWorldPosX(), getChunkWorldPosY(), getChunkWorldPosZ());
    }

    public int getChunkWorldPosX() {
        return pos.x * getChunkSizeX();
    }

    public int getChunkWorldPosY() {
        return pos.y * getChunkSizeY() * chunkType.fStackable;
    }

    public int getChunkWorldPosZ() {
        return pos.z * getChunkSizeZ();
    }

    public Vector3i getBlockWorldPos(Vector3i blockPos) {
        return getBlockWorldPos(blockPos.x, blockPos.y, blockPos.z);
    }

    public Vector3i getBlockWorldPos(int x, int y, int z) {
        return new Vector3i(getBlockWorldPosX(x), getBlockWorldPosY(y), getBlockWorldPosZ(z));
    }

    public int getBlockWorldPosX(int x) {
        return x + getChunkWorldPosX();
    }

    public int getBlockWorldPosY(int y) {
        return y + getChunkWorldPosY();
    }

    public int getBlockWorldPosZ(int z) {
        return z + getChunkWorldPosZ();
    }

    public AABB getAABB() {
        if (aabb == null) {
            Vector3f dimensions = new Vector3f(0.5f * getChunkSizeX(), 0.5f * getChunkSizeY(), 0.5f * getChunkSizeZ());
            Vector3f position = new Vector3f(getChunkWorldPosX() + dimensions.x - 0.5f, getChunkWorldPosY() + dimensions.y - 0.5f, getChunkWorldPosZ() + dimensions.z - 0.5f);
            aabb = AABB.createCenterExtent(position, dimensions);
        }

        return aabb;
    }

    @Deprecated
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeInt(pos.x);
        out.writeInt(pos.y);
        out.writeInt(pos.z);
        out.writeObject(chunkState);
        out.writeObject(blockData);
        out.writeObject(sunlightData);
        out.writeObject(lightData);
        out.writeObject(extraData);
    }

    @Deprecated
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        pos.x = in.readInt();
        pos.y = in.readInt();
        pos.z = in.readInt();
        setDirty(true);
        chunkState = (ChunkState) in.readObject();
        blockData = (TeraArray) in.readObject();
        sunlightData = (TeraArray) in.readObject();
        lightData = (TeraArray) in.readObject();
        extraData = (TeraArray) in.readObject();
    }
    
    private static DecimalFormat fpercent = new DecimalFormat("0.##");
    private static DecimalFormat fsize = new DecimalFormat("#,###");
    public void deflate() {
        if (getChunkState() != ChunkState.COMPLETE) {
            logger.warn("Before deflation the state of the chunk ({}, {}, {}) should be set to State.COMPLETE but is now State.{}", getPos().x, getPos().y, getPos().z, getChunkState().toString());
        }
        lock();
        try {
            AdvancedConfig config = CoreRegistry.get(org.terasology.config.Config.class).getAdvancedConfig();
            final TeraDeflator def = new TeraStandardDeflator();
            
            if (config.isChunkDeflationLoggingEnabled()) {
                int blocksSize = blockData.getEstimatedMemoryConsumptionInBytes();
                int sunlightSize = sunlightData.getEstimatedMemoryConsumptionInBytes();
                int lightSize = lightData.getEstimatedMemoryConsumptionInBytes();
                int liquidSize = extraData.getEstimatedMemoryConsumptionInBytes();
                int totalSize = blocksSize + sunlightSize + lightSize + liquidSize;

                blockData = def.deflate(blockData);
                sunlightData = def.deflate(sunlightData);
                lightData = def.deflate(lightData);
                extraData = def.deflate(extraData);

                int blocksReduced = blockData.getEstimatedMemoryConsumptionInBytes();
                int sunlightReduced = sunlightData.getEstimatedMemoryConsumptionInBytes();
                int lightReduced = lightData.getEstimatedMemoryConsumptionInBytes();
                int liquidReduced = extraData.getEstimatedMemoryConsumptionInBytes();
                int totalReduced = blocksReduced + sunlightReduced + lightReduced + liquidReduced;

                double blocksPercent = 100d - (100d / blocksSize * blocksReduced);
                double sunlightPercent = 100d - (100d / sunlightSize * sunlightReduced);
                double lightPercent = 100d - (100d / lightSize * lightReduced);
                double liquidPercent = 100d - (100d / liquidSize * liquidReduced);
                double totalPercent = 100d - (100d / totalSize * totalReduced);

                ChunkMonitor.getEventBus().post(new ChunkEvent.Deflated(this, totalSize, totalReduced));
                
                logger.info(String.format("chunk (%d, %d, %d): size-before: %s bytes, size-after: %s bytes, total-deflated-by: %s%%, blocks-deflated-by=%s%%, sunlight-deflated-by=%s%%, light-deflated-by=%s%%, liquid-deflated-by=%s%%", pos.x, pos.y, pos.z, fsize.format(totalSize), fsize.format(totalReduced), fpercent.format(totalPercent), fpercent.format(blocksPercent), fpercent.format(sunlightPercent), fpercent.format(lightPercent), fpercent.format(liquidPercent)));
            } else {
                final int oldSize = getEstimatedMemoryConsumptionInBytes();
                blockData = def.deflate(blockData);
                sunlightData = def.deflate(sunlightData);
                lightData = def.deflate(lightData);
                extraData = def.deflate(extraData);
                ChunkMonitor.getEventBus().post(new ChunkEvent.Deflated(this, oldSize, getEstimatedMemoryConsumptionInBytes()));
            }
        } finally {
            unlock();
        }
    }

    @Override
    public String toString() {
        return "Chunk" + pos.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(pos);
    }

    public void setMesh(ChunkMesh[] mesh) {
        this.mesh = mesh;
    }

    public void setPendingMesh(ChunkMesh[] mesh) {
        this.pendingMesh = mesh;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public boolean getAnimated() {
        return animated;
    }


    public ChunkMesh[] getMesh() {
        return mesh;
    }

    public ChunkMesh[] getPendingMesh() {
        return pendingMesh;
    }

    public AABB getSubMeshAABB(int subMesh) {
        if (subMeshAABB == null) {
            subMeshAABB = new AABB[VERTICAL_SEGMENTS];

            int heightHalf = chunkType.sizeY / VERTICAL_SEGMENTS / 2;

            for (int i = 0; i < subMeshAABB.length; i++) {
                Vector3f dimensions = new Vector3f(8, heightHalf, 8);
                Vector3f position = new Vector3f(getChunkWorldPosX() + dimensions.x - 0.5f, getChunkWorldPosY() + (i * heightHalf * 2) + dimensions.y - 0.5f, getChunkWorldPosZ() + dimensions.z - 0.5f);
                subMeshAABB[i] = AABB.createCenterExtent(position, dimensions);
            }
        }

        return subMeshAABB[subMesh];
    }

    public void dispose() {
        disposed = true;
        if (mesh != null) {
            for (ChunkMesh chunkMesh : mesh) {
                chunkMesh.dispose();
            }
            mesh = null;
        }
        ChunkMonitor.getEventBus().post(new ChunkEvent.Disposed(this));
    }

    public boolean isDisposed() {
        return disposed;
    }

    public int getChunkSizeX() {
        return chunkType.sizeX;
    }

    public int getChunkSizeY() {
        return chunkType.sizeY;
    }

    public int getChunkSizeZ() {
        return chunkType.sizeZ;
    }
}
