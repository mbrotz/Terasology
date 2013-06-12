package org.terasology.logic.mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.world.chunks.blockdata.TeraArrays;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * ModDataExtensionRegistry is the central registration point for mod data extensions.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
public class ModDataExtensionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ModDataExtensionRegistry.class);
    private static final ModDataExtensionRegistry instance = new ModDataExtensionRegistry();

    private final Map<String, ModDataExtension> extensions = Maps.newHashMap();
    
    private ModDataExtensionRegistry() {}
    
    public ModDataExtension getEntry(String id) {
        Preconditions.checkNotNull(id, "The parameter 'id' must not be null");
        return extensions.get(id);
    }
    
    public List<ModDataExtension> getEntries() {
        return new ArrayList<ModDataExtension>(extensions.values());
    }
    
    public boolean enableModExtensions(Mod mod) {
        Preconditions.checkNotNull(mod, "The parameter 'mod' must not be null");
        final ModInfo info = mod.getModInfo();
        for (ModDataExtensionInfo extInfo : info.getDataExtensions().values()) {
            if (extensions.containsKey(extInfo.getId())) {
                final ModInfo other = extensions.get(extInfo.getId()).getMod().getModInfo();
                if (!other.getId().equals(info.getId())) {
                    logger.error("Cannot activate mod data extension '{}' for mod '{}' due to another mod '{}' using the same id", extInfo.getId(), info.getDisplayName(), other.getDisplayName());
                    disableModExtensions(mod);
                    return false;
                }
            } else {
                final TeraArrays.Entry array = TeraArrays.getInstance().getEntry(extInfo.getArrayClassName());
                if (array != null) {
                    extensions.put(extInfo.getId(), new ModDataExtension(mod, extInfo, array));
                    logger.info("Activated mod data extension '{}' for mod '{}' with array class '{}'", extInfo.getId(), info.getDisplayName(), extInfo.getArrayClassName());
                } else {
                    logger.error("Cannot activate mod data extension '{}' for mod '{}' due to an unknown array class '{}'", extInfo.getId(), info.getDisplayName(), extInfo.getArrayClassName());
                    disableModExtensions(mod);
                    return false;
                }
            }
        }
        return true;
    }
    
    public void disableModExtensions(Mod mod) {
        final Iterator<ModDataExtension> it = extensions.values().iterator();
        while (it.hasNext()) {
            final ModDataExtension ext = it.next();
            if (ext.getMod() == mod)
                it.remove();
        }
    }
    
    public static ModDataExtensionRegistry getInstance() {
        return instance;
    }
}
