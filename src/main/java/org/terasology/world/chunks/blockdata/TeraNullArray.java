package org.terasology.world.chunks.blockdata;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.terasology.world.chunks.deflate.TeraVisitingDeflator;

/**
 * TeraNullArray returns always zero and consumes any input.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public final class TeraNullArray extends TeraArray {

    @Override
    protected void initialize() {}

    public TeraNullArray(int sizeX, int sizeY, int sizeZ) {
        super(sizeX, sizeY, sizeZ, false);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {}

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {}

    @Override
    public boolean isSparse() {
        return false;
    }

    @Override
    public TeraArray copy() {
        return new TeraNullArray(getSizeX(), getSizeY(), getSizeZ());
    }

    @Override
    public TeraArray deflate(TeraVisitingDeflator deflator) {
        return null;
    }

    @Override
    public int getEstimatedMemoryConsumptionInBytes() {
        return 0;
    }

    @Override
    public int getElementSizeInBits() {
        return 0;
    }

    @Override
    public int get(int x, int y, int z) {
        return 0;
    }

    @Override
    public int set(int x, int y, int z, int value) {
        return 0;
    }

    @Override
    public boolean set(int x, int y, int z, int value, int expected) {
        return false;
    }

    @Override
    public void fill(int value) {}
}
