package org.terasology.world.chunks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.world.chunks.blockdata.TeraArray;
import org.terasology.world.chunks.blockdata.TeraArrays;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * ModDataExtensionsManager is the central registration point for mod data extensions.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public class ModDataExtensionsManager {

    protected static final Logger logger = LoggerFactory.getLogger(ModDataExtensionsManager.class);
    
    protected final Map<String, ModDataExtension> extensions = Maps.newHashMap();
    
    protected ModDataExtension registerExtension(String id, TeraArrays.Entry entry, boolean fill, int fillValue) {
        Preconditions.checkNotNull(id, "The parameter 'id' must not be null");
        Preconditions.checkArgument(!id.trim().isEmpty(), "The parameter 'id' must not be empty");
        Preconditions.checkNotNull(entry, "The parameter 'entry' must not be null");
        Preconditions.checkState(!extensions.containsKey(id), "The mod data id '" + id + "' is already in use");
        final ModDataExtension result = new ModDataExtension(id, entry, fill, fillValue);
        extensions.put(id, result);
        logger.info("Registered mod data extension with id = {}, array class = {}, fill = {}, fillValue = {}", id, entry.arrayClass.getSimpleName(), fill, fillValue);
        return result;
    }

    public ModDataExtensionsManager() {}
    
    public static class ModDataExtension {
        
        public final String id;
        public final TeraArrays.Entry entry;
        public final boolean fill;
        public final int fillValue;
        
        private ModDataExtension(String id, TeraArrays.Entry entry, boolean fill, int fillValue) {
            this.id = Preconditions.checkNotNull(id, "The parameter 'id' must not be null");
            this.entry = Preconditions.checkNotNull(entry, "The parameter 'entry' must not be null");
            this.fill = fill;
            this.fillValue = fillValue;
        }
        
        public TeraArray create(int sizeX, int sizeY, int sizeZ) {
            final TeraArray result = entry.factory.create(sizeX, sizeY, sizeZ);
            if (fill) result.fill(fillValue);
            return result;
        }
    }
    
    public ModDataExtension getExtension(String id) {
        Preconditions.checkNotNull(id, "The parameter 'id' must not be null");
        return extensions.get(id);
    }
    
    public List<ModDataExtension> getExtensions() {
        return new ArrayList<ModDataExtension>(extensions.values());
    }
    
    public ModDataExtension registerExtension(String id, TeraArrays.Entry entry, int fillValue) {
        return registerExtension(id, entry, true, fillValue);
    }
    
    public ModDataExtension registerExtension(String id, TeraArrays.Entry entry) {
        return registerExtension(id, entry, false, 0);
    }
}
