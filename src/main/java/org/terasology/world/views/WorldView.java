package org.terasology.world.views;

import org.terasology.math.Vector3i;

/**
 * WorldView is the abstract base class used to implement all kinds of views.
 * <p/>
 * A view can span one or more chunks and aquires all locks of its underlying chunks at creation.
 * <p/>
 * In preparation for Java 7, a view's locks are released through the method close(). 
 * In the future, the class will implement the interface AutoCloseable to support automatic resource cleanup.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public abstract class WorldView {
    
    /**
     * @return Returns the number of chunks this view spans.
     */
    public abstract int getSize();
    
    /**
     * @return Returns the coordinates of the chunk with the smallest x, y and z coordinates.
     */
    public abstract Vector3i getMinChunk();
    
    /**
     * @return Returns the coordinates of the chunk with the largest x, y and z coordinates.
     */
    public abstract Vector3i getMaxChunk();
    
    /**
     * @return Returns the world coordinates of the block with the smallest x, y and z coordinates.
     */
    public abstract Vector3i getMinWorldBlockPos();
    
    /**
     * @return Returns the world coordinates of the block with the largest x, y and z coordinates.
     */
    public abstract Vector3i getMaxWorldBlockPos();

    /**
     * @return Returns true, if this view is read only, false otherwise.
     */
    public abstract boolean isReadOnly();

    /**
     * @return Returns true, if this view is valid and can be used, false otherwise.
     */
    public abstract boolean isValid();

    /**
     * Releases all locks held by this view. After calling this method, the view is no longer valid.
     */
    public abstract void close();
}
