package org.terasology.world.chunks;

import org.terasology.math.Region3i;
import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;

/**
 * ChunkType allows the definition of different kinds of chunks.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public enum ChunkType {

    Classic(16, 256, 16, 0, false, true) {
        
        @Override
        public int calcChunkPosY(int worldBlockPosY) {
            return 0;
        }
        
        @Override
        public int calcBlockPosY(int worldBlockPosY) {
            return worldBlockPosY;
        }
    }, 

    Small(16, 128, 16, 1, false, true) {
        
        @Override
        public int calcChunkPosY(int worldBlockPosY) {
            return 0;
        }
        
        @Override
        public int calcBlockPosY(int worldBlockPosY) {
            return worldBlockPosY;
        }
    }, 

    Tall(16, 512, 16, 2, false, true) {
        
        @Override
        public int calcChunkPosY(int worldBlockPosY) {
            return 0;
        }
        
        @Override
        public int calcBlockPosY(int worldBlockPosY) {
            return worldBlockPosY;
        }
    }, 
    
    Stackable(16, 16, 16, 31, true, true),

    Miniature(512, 256, 512, 127, false, false){
        
        @Override
        public int calcChunkPosY(int worldBlockPosY) {
            return 0;
        }
        
        @Override
        public int calcBlockPosY(int worldBlockPosY) {
            return worldBlockPosY;
        }
    };
    
    private ChunkType(int sizeX, int sizeY, int sizeZ, int id, boolean isStackable, boolean isSelectable) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.id = id;
        this.isStackable = isStackable;
        this.fStackable = isStackable ? 1 : 0;
        this.isSelectable = isSelectable;
        this.chunkPosFilterX = TeraMath.ceilPowerOfTwo(sizeX) - 1;
        this.chunkPosFilterY = isStackable ? TeraMath.ceilPowerOfTwo(sizeY) - 1 : 0;
        this.chunkPosFilterZ = TeraMath.ceilPowerOfTwo(sizeZ) - 1;
        this.powerX = TeraMath.sizeOfPower(sizeX);
        this.powerY = isStackable ? TeraMath.sizeOfPower(sizeY) : 0;
        this.powerZ = TeraMath.sizeOfPower(sizeZ);
    }
    
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    
    public final int id;
    
    public final boolean isStackable;
    public final int fStackable;
    
    public final boolean isSelectable;
    
    public final int chunkPosFilterX;
    public final int chunkPosFilterY;
    public final int chunkPosFilterZ;
    
    public final int powerX;
    public final int powerY;
    public final int powerZ;
    
    public final Vector3i getChunkSize() {
        return new Vector3i(sizeX, sizeY, sizeZ);
    }
    
    public final Vector3i getChunkPower() {
        return new Vector3i(powerX, powerY, powerZ);
    }
    
    public final Vector3i getChunkPosFilter() {
        return new Vector3i(chunkPosFilterX, chunkPosFilterY, chunkPosFilterZ);
    }
    

    /**
     * Returns the chunk position of a given coordinate.
     * @param worldBlockPosX The X-coordinate of the block in the world
     * @return The X-coordinate of the chunk
     */
    public int calcChunkPosX(int worldBlockPosX) {
        return worldBlockPosX >> powerX;
    }

    /**
     * Returns the chunk position of a given coordinate
     * @param worldBlockPosY The Y-coordinate of the block in the world
     * @return The Y-coordinate of the chunk
     */
    public int calcChunkPosY(int worldBlockPosY) {
        return worldBlockPosY >> powerY;
    }
    
    /**
     * Returns the chunk position of a given coordinate.
     * @param worldBlockPosZ The Z-coordinate of the block in the world
     * @return The Z-coordinate of the chunk
     */
    public int calcChunkPosZ(int worldBlockPosZ) {
        return worldBlockPosZ >> powerZ;
    }
    
    /**
     * Returns the chunk position of a given coordinate.
     * @param worldBlockPosX The X-coordinate of the block in the world
     * @return The X-coordinate of the chunk
     */
    public int calcChunkPosX(float worldBlockPosX) {
        return TeraMath.floorToInt(worldBlockPosX) >> powerX;
    }

    /**
     * Returns the chunk position of a given coordinate
     * @param worldBlockPosY The Y-coordinate of the block in the world
     * @return The Y-coordinate of the chunk
     */
    public int calcChunkPosY(float worldBlockPosY) {
        return TeraMath.floorToInt(worldBlockPosY) >> powerY;
    }
    
    /**
     * Returns the chunk position of a given coordinate.
     * @param worldBlockPosZ The Z-coordinate of the block in the world
     * @return The Z-coordinate of the chunk
     */
    public int calcChunkPosZ(float worldBlockPosZ) {
        return TeraMath.floorToInt(worldBlockPosZ) >> powerZ;
    }
    
    /**
     * Returns the chunk position of a given block position in the world.
     * @param blockWorldPos The position of the block in the world
     * @param output The vector recieving the result (must not be null)
     * @return The passed parameter {@code output}
     */
    public final Vector3i calcChunkPos(Vector3i blockWorldPos, Vector3i output) {
        output.set(calcChunkPosX(blockWorldPos.x), calcChunkPosY(blockWorldPos.y), calcChunkPosZ(blockWorldPos.z));
        return output;
    }
    
    /**
     * Returns the chunk position of a given block position in the world.
     * @param blockWorldPos The position of the block in the world
     * @return The position of the chunk
     */
    public final Vector3i calcChunkPos(Vector3i blockWorldPos) {
        return new Vector3i(calcChunkPosX(blockWorldPos.x), calcChunkPosY(blockWorldPos.y), calcChunkPosZ(blockWorldPos.z));
    }
    
    /**
     * Returns the chunk position of a given block position in the world.
     * @param x The X-coordinate of the block in the world
     * @param y The Y-coordinate of the block in the world
     * @param z The Z-coordinate of the block in the world
     * @param output The vector recieving the result (must not be null)
     * @return The passed parameter {@code output}
     */
    public final Vector3i calcChunkPos(int x, int y, int z, Vector3i output) {
        output.set(calcChunkPosX(x), calcChunkPosY(y), calcChunkPosZ(z));
        return output;
    }
    
    /**
     * Returns the chunk position of a given block position in the world.
     * @param x The X-coordinate of the block in the world
     * @param y The Y-coordinate of the block in the world
     * @param z The Z-coordinate of the block in the world
     * @return The position of the chunk
     */
    public final Vector3i calcChunkPos(int x, int y, int z) {
        return new Vector3i(calcChunkPosX(x), calcChunkPosY(y), calcChunkPosZ(z));
    }
    
    /**
     * Returns the chunk position of a given block position in the world.
     * @param x The X-coordinate of the block in the world
     * @param y The Y-coordinate of the block in the world
     * @param z The Z-coordinate of the block in the world
     * @return The position of the chunk
     */
    public final Vector3i calcChunkPos(float x, float y, float z) {
        return new Vector3i(calcChunkPosX(x), calcChunkPosY(y), calcChunkPosZ(z));
    }
    
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPosX The X-coordinate of the block in the world
     * @return The X-coordinate of the block within the chunk
     */
    public int calcBlockPosX(int worldBlockPosX) {
        return worldBlockPosX & chunkPosFilterX;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPosY The Y-coordinate of the block in the world
     * @return The Y-coordinate of the block within the chunk
     */
    public int calcBlockPosY(int worldBlockPosY) {
        return worldBlockPosY & chunkPosFilterY;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPosZ The Z-coordinate of the block in the world
     * @return The Z-coordinate of the block within the chunk
     */
    public int calcBlockPosZ(int worldBlockPosZ) {
        return worldBlockPosZ & chunkPosFilterZ;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPosX The X-coordinate of the block in the world
     * @return The X-coordinate of the block within the chunk
     */
    public int calcBlockPosX(float worldBlockPosX) {
        return TeraMath.floorToInt(worldBlockPosX) & chunkPosFilterX;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPosY The Y-coordinate of the block in the world
     * @return The Y-coordinate of the block within the chunk
     */
    public int calcBlockPosY(float worldBlockPosY) {
        return TeraMath.floorToInt(worldBlockPosY) & chunkPosFilterY;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPosZ The Z-coordinate of the block in the world
     * @return The Z-coordinate of the block within the chunk
     */
    public int calcBlockPosZ(float worldBlockPosZ) {
        return TeraMath.floorToInt(worldBlockPosZ) & chunkPosFilterZ;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPos The position of the block in the world
     * @param output The vector recieving the result (must not be null)
     * @return The passed parameter {@code output}
     */
    public final Vector3i calcBlockPos(Vector3i blockWorldPos, Vector3i output) {
        output.set(calcBlockPosX(blockWorldPos.x), calcBlockPosY(blockWorldPos.y), calcBlockPosZ(blockWorldPos.z));
        return output;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param worldBlockPos The position of the block in the world
     * @return The position of the block
     */
    public final Vector3i calcBlockPos(Vector3i blockWorldPos) {
        return new Vector3i(calcBlockPosX(blockWorldPos.x), calcBlockPosY(blockWorldPos.y), calcBlockPosZ(blockWorldPos.z));
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param x The X-coordinate of the block in the world
     * @param y The Y-coordinate of the block in the world
     * @param z The Z-coordinate of the block in the world
     * @param output The vector recieving the result (must not be null)
     * @return The passed parameter {@code output}
     */
    public final Vector3i calcBlockPos(int x, int y, int z, Vector3i output) {
        output.set(calcBlockPosX(x), calcBlockPosY(y), calcBlockPosZ(z));
        return output;
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param x The X-coordinate of the block in the world
     * @param y The Y-coordinate of the block in the world
     * @param z The Z-coordinate of the block in the world
     * @return The position of the block
     */
    public final Vector3i calcBlockPos(int x, int y, int z) {
        return new Vector3i(calcBlockPosX(x), calcBlockPosY(y), calcBlockPosZ(z));
    }
    
    /**
     * Returns the internal position of a block within a chunk.
     * @param x The X-coordinate of the block in the world
     * @param y The Y-coordinate of the block in the world
     * @param z The Z-coordinate of the block in the world
     * @return The position of the block
     */
    public final Vector3i calcBlockPos(float x, float y, float z) {
        return new Vector3i(calcBlockPosX(x), calcBlockPosY(y), calcBlockPosZ(z));
    }
    
    
    /**
     * 
     * @param worldBlockPos The position of a block in the world
     * @param extent The radius of blocks around the given block position
     * @return The region of chunks around the block
     */
    public final Region3i getChunkRegionAroundBlockPos(Vector3i worldBlockPos, int extent) {
        final int minX = worldBlockPos.x - extent, minY = worldBlockPos.y - (extent * fStackable), minZ = worldBlockPos.z - extent;
        final int maxX = worldBlockPos.x + extent, maxY = worldBlockPos.y + (extent * fStackable), maxZ = worldBlockPos.z + extent;
        final Vector3i minChunk = calcChunkPos(minX, minY, minZ);
        final Vector3i maxChunk = calcChunkPos(maxX, maxY, maxZ);
        return Region3i.createFromMinMax(minChunk, maxChunk);
    }

    public final Region3i getChunkRegionAroundBlockRegion(Region3i worldBlockRegion, int extent) {
        final Vector3i minPos = worldBlockRegion.min();
        minPos.sub(extent, extent * fStackable, extent);
        final Vector3i maxPos = worldBlockRegion.max();
        maxPos.add(extent, extent * fStackable, extent);
        final Vector3i minChunk = calcChunkPos(minPos, minPos);
        final Vector3i maxChunk = calcChunkPos(maxPos, maxPos);
        return Region3i.createFromMinMax(minChunk, maxChunk);
    }
    
    public final Vector3i getChunkExtents(int extent) {
        return new Vector3i(extent, extent * fStackable, extent);
    }
    
    
    public static final ChunkType getTypeById(int id) {
        switch (id) {
        case 0: return Classic;
        case 1: return Small;
        case 2: return Tall;
        case 31: return Stackable;
        case 127: return Miniature;
        }
        return null;
    }
}
