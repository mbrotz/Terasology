package org.terasology.world.chunks.perBlockStorage;

/**
 * This is the interface for tera array factories. Every tera array is required to implement a factory.
 * It should be implemented as a static subclass of the corresponding tera array class and it should be called Factory.
 *  
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 * @see org.terasology.world.chunks.perBlockStorage.TeraDenseArray16Bit.Factory
 *
 */
public interface PerBlockStorageFactory {

    public String getId();
    
    public TeraArray create();
    
    public TeraArray create(int sizeX, int sizeY, int sizeZ);
    
}