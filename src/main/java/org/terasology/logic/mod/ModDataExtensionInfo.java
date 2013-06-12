package org.terasology.logic.mod;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;

/**
 * Information on a mod data extension
 *
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 */
public class ModDataExtensionInfo {
    
    private final String id;
    private final String arrayClassName;
    private final boolean fill;
    private final int fillValue;
    
    public ModDataExtensionInfo(JsonObject info) {
        Preconditions.checkNotNull(info, "The parameter 'info' must not be null");
        if (info.has("id"))
            this.id = info.get("id").getAsString().trim();
        else
            this.id = "";
        if (info.has("arrayClassName"))
            this.arrayClassName = info.get("arrayClassName").getAsString();
        else
            this.arrayClassName = "";
        if (info.has("fill") && info.get("fill").isJsonPrimitive())
            this.fill = info.get("fill").getAsBoolean();
        else
            this.fill = false;
        if (info.has("fillValue") && info.get("fillValue").isJsonPrimitive())
            this.fillValue = info.get("fillValue").getAsInt();
        else
            this.fillValue = 0;
    }
    
    public ModDataExtensionInfo(String id, String arrayClassName, boolean fill, int fillValue) {
        this.id = Preconditions.checkNotNull(id, "The parameter 'id' must not be null");
        this.arrayClassName = Preconditions.checkNotNull(arrayClassName, "The parameter 'arrayClassName' must not be null");
        this.fill = fill;
        this.fillValue = fillValue;
    }
    
    public String getId() {
        return id;
    }
    
    public String getArrayClassName() {
        return arrayClassName;
    }
    
    public boolean getFill() {
        return fill;
    }
    
    public int getFillValue() {
        return fillValue;
    }
}