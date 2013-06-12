package org.terasology.logic.mod;

import org.terasology.world.chunks.blockdata.TeraArray;
import org.terasology.world.chunks.blockdata.TeraArrays;

import com.google.common.base.Preconditions;

public class ModDataExtension {
    
    private final Mod mod;
    private final ModDataExtensionInfo info;
    private final TeraArrays.Entry entry;
    
    public ModDataExtension(Mod mod, ModDataExtensionInfo extension, TeraArrays.Entry array) {
        this.mod = Preconditions.checkNotNull(mod, "The parameter 'mod' must not be null");
        this.info = Preconditions.checkNotNull(extension, "The parameter 'extension' must not be null");
        this.entry = Preconditions.checkNotNull(array, "The parameter 'array' must not be null");
        Preconditions.checkState(mod.getModInfo().getDataExtensions().containsValue(extension), "The supplied mod is not the owner of the supplied mod data extension info (mod id = '" + mod.getModInfo().getId() + "', mod data extension id = '" + extension.getId() + "')");
        Preconditions.checkState(extension.getArrayClassName().equals(array.arrayClassName), "The values of 'extension.getArrayClassName()' and 'array.arrayClassName' must be equal ('" + extension.getArrayClassName() + "', '" + array.arrayClassName + "')");
    }

    public Mod getMod() {
        return mod;
    }
    
    public ModDataExtensionInfo getInfo() {
        return info;
    }
    
    public TeraArrays.Entry getArrayEntry() {
        return entry;
    }
    
    public TeraArray create(int sizeX, int sizeY, int sizeZ) {
        final TeraArray result = entry.factory.create(sizeX, sizeY, sizeZ);
        if (info.getFill()) result.fill(info.getFillValue());
        return result;
    }
}