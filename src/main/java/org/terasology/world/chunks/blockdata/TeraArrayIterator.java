package org.terasology.world.chunks.blockdata;

import org.terasology.math.Vector3i;

import com.google.common.base.Preconditions;

import gnu.trove.iterator.TIntIntIterator;

/**
 * TeraArrayIterator allows iteration over the contents of tera arrays.
 * 
 * @author mbrotz
 *
 */
public final class TeraArrayIterator implements TIntIntIterator {

    private final TeraArray array;
    private final int sizeX, sizeZ, sizeXYZ;
    private int i = 0, x = 0, y = 0, z = 0;
    
    public TeraArrayIterator(final TeraArray array) {
        Preconditions.checkNotNull(array, "The parameter 'array' must not be null");
        this.array = array;
        this.sizeX = array.getSizeX();
        this.sizeZ = array.getSizeZ();
        this.sizeXYZ = array.getSizeXYZ();
    }

    /**
     * Advances the iterator to the next array element. If there are no more
     * elements left, an IndexOutOfBoundsException is thrown.
     * 
     * @throws IndexOutOfBoundsException
     */
    @Override
    public void advance() {
        if (i >= sizeXYZ)
            throw new IndexOutOfBoundsException();
        ++i;
        ++x;
        if (x >= sizeX) {
            x = 0;
            ++z;
            if (z >= sizeZ) {
                z = 0;
                ++y;
            }
        }
    }

    @Override
    public boolean hasNext() {
        return i < sizeXYZ;
    }

    /**
     * Removal is not supported.
     * 
     * @throws UnsupportedOperationException
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * The method key() returns the index of the current array element.
     * 
     * @return The index of the current array element.
     */
    @Override
    public int key() {
        return i;
    }
    
    public int x() {
        return x;
    }
    
    public int y() {
        return y;
    }
    
    public int z() {
        return z;
    }
    
    public Vector3i pos() {
        return new Vector3i(x, y, z);
    }

    @Override
    public int value() {
        return array.get(x, y, z);
    }

    @Override
    public int setValue(int value) {
        return array.set(x, y, z, value);
    }
}
