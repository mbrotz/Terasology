package org.terasology.world.chunks;

import org.terasology.math.TeraMath;
import org.terasology.math.Vector3i;

public enum ChunkType {

    Classic(16, 256, 16, 0, false), Miniature(512, 256, 512, 1, false);

    public static final ChunkType Default = ChunkType.Classic;
    
    private ChunkType(int sizeX, int sizeY, int sizeZ, int id, boolean isStackable) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.id = id;
        this.isStackable = isStackable;
        this.innerChunkPosFilterX = TeraMath.ceilPowerOfTwo(sizeX) - 1;
        this.innerChunkPosFilterY = isStackable ? TeraMath.ceilPowerOfTwo(sizeY) - 1 : 0;
        this.innerChunkPosFilterZ = TeraMath.ceilPowerOfTwo(sizeZ) - 1;
        this.powerX = TeraMath.sizeOfPower(sizeX);
        this.powerY = isStackable ? TeraMath.sizeOfPower(sizeY) : 0;
        this.powerZ = TeraMath.sizeOfPower(sizeZ);
    }
    
    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;
    
    public final int id;
    
    public final boolean isStackable;
    
    public final int innerChunkPosFilterX;
    public final int innerChunkPosFilterY;
    public final int innerChunkPosFilterZ;
    
    public final int powerX;
    public final int powerY;
    public final int powerZ;
    
    public final Vector3i getChunkSize() {
        return new Vector3i(sizeX, sizeY, sizeZ);
    }
    
    public final Vector3i getChunkPower() {
        return new Vector3i(powerX, powerY, powerZ);
    }
    
    public final Vector3i getInnerChunkPosFilter() {
        return new Vector3i(innerChunkPosFilterX, innerChunkPosFilterY, innerChunkPosFilterZ);
    }
    
    public static final ChunkType getTypeById(int id) {
        switch (id) {
        case 0: return Classic;
        case 1: return Miniature;
        }
        return null;
    }
}
