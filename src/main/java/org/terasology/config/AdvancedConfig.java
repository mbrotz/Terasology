package org.terasology.config;

import java.lang.reflect.Type;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.world.chunks.blockdata.TeraArray;
import org.terasology.world.chunks.blockdata.TeraArrays;
import org.terasology.world.chunks.blockdata.TeraDenseArray8Bit;

import com.google.common.base.Preconditions;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Allows to configure internal details of the Terasology engine.
 * 
 * @author Manuel Brotz <manu.brotz@gmx.ch>
 *
 */
@SuppressWarnings("rawtypes")
public final class AdvancedConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedConfig.class);

    private String blockDataClassName, sunlightDataClassName, lightDataClassName, extraDataClassName;
    private boolean chunkDeflationEnabled, chunkDeflationLoggingEnabled;
    private boolean advancedMonitoringEnabled, advancedMonitorVisibleAtStartup;
    
    private static TeraArray.Factory getTeraArrayFactory(String arrayClassName) {
        Preconditions.checkNotNull(arrayClassName, "The parameter 'arrayClassName' must not be null");
        final TeraArrays.Entry entry = TeraArrays.getInstance().getEntry(arrayClassName);
        if (entry != null)
            return entry.factory;
        return null;
    }

    private static void checkSupportsTeraArrayClass(String arrayClassName) {
        Preconditions.checkNotNull(arrayClassName, "Parameter 'arrayClassName' must not be null");
        Preconditions.checkState(supportsTeraArrayClass(arrayClassName), "TeraArray class not supported: '" + arrayClassName + "'");
    }
    
    private static boolean supportsTeraArrayClass(String factory) {
        return factory != null && getTeraArrayFactory(factory) != null;
    }
    
    private AdvancedConfig() {}
    
    public String getBlockDataClassName() {
        return blockDataClassName;
    }
    
    public AdvancedConfig setBlockDataClassName(String arrayClassName) {
        checkSupportsTeraArrayClass(arrayClassName);
        blockDataClassName = arrayClassName;
        return this;
    }
    
    public AdvancedConfig setBlockDataClassNameDontThrow(String arrayClassName) {
        if (supportsTeraArrayClass(arrayClassName)) {
            blockDataClassName = arrayClassName;
        } else {
            logger.warn("TeraArray class does not exist: '{}'", arrayClassName);
        }
        return this;
    }
    
    public String getSunlightDataClassName() {
        return sunlightDataClassName;
    }
    
    public AdvancedConfig setSunlightDataClassName(String arrayClassName) {
        checkSupportsTeraArrayClass(arrayClassName);
        sunlightDataClassName = arrayClassName;
        return this;
    }
    
    public AdvancedConfig setSunlightDataClassNameDontThrow(String arrayClassName) {
        if (supportsTeraArrayClass(arrayClassName)) {
            sunlightDataClassName = arrayClassName;
        } else {
            logger.warn("TeraArray class does not exist: '{}'", arrayClassName);
        }
        return this;
    }
    
    public String getLightDataClassName() {
        return lightDataClassName;
    }
    
    public AdvancedConfig setLightDataClassName(String arrayClassName) {
        checkSupportsTeraArrayClass(arrayClassName);
        lightDataClassName = arrayClassName;
        return this;
    }
    
    public AdvancedConfig setLightDataClassNameDontThrow(String arrayClassName) {
        if (supportsTeraArrayClass(arrayClassName)) {
            lightDataClassName = arrayClassName;
        } else {
            logger.warn("TeraArray class does not exist: '{}'", arrayClassName);
        }
        return this;
    }
    
    public String getExtraDataClassName() {
        return extraDataClassName;
    }
    
    public AdvancedConfig setExtraDataClassName(String arrayClassName) {
        checkSupportsTeraArrayClass(arrayClassName);
        extraDataClassName = arrayClassName;
        return this;
    }
    
    public AdvancedConfig setExtraDataClassNameDontThrow(String arrayClassName) {
        if (supportsTeraArrayClass(arrayClassName)) {
            extraDataClassName = arrayClassName;
        } else {
            logger.warn("TeraArray class does not exist: '{}'", arrayClassName);
        }
        return this;
    }
    
    public boolean isChunkDeflationEnabled() {
        return chunkDeflationEnabled;
    }
    
    public AdvancedConfig setChunkDeflationEnabled(boolean enabled) {
        chunkDeflationEnabled = enabled;
        return this;
    }
    
    public boolean isChunkDeflationLoggingEnabled() {
        return chunkDeflationLoggingEnabled;
    }
    
    public AdvancedConfig setChunkDeflationLoggingEnabled(boolean enabled) {
        chunkDeflationLoggingEnabled = enabled;
        return this;
    }
    

    public boolean isAdvancedMonitoringEnabled() {
        return advancedMonitoringEnabled;
    }

    public AdvancedConfig setAdvancedMonitoringEnabled(boolean enabled) {
        advancedMonitoringEnabled = enabled;
        return this;
    }

    public boolean isAdvancedMonitorVisibleAtStartup() {
        return advancedMonitorVisibleAtStartup;
    }

    public AdvancedConfig setAdvancedMonitorVisibleAtStartup(boolean visible) {
        advancedMonitorVisibleAtStartup = visible;
        return this;
    }

    public static AdvancedConfig createDefault() {
        return new AdvancedConfig()
        .setBlockDataClassName(TeraDenseArray8Bit.class.getName())
        .setSunlightDataClassName(TeraDenseArray8Bit.class.getName())
        .setLightDataClassName(TeraDenseArray8Bit.class.getName())
        .setExtraDataClassName(TeraDenseArray8Bit.class.getName())
        .setChunkDeflationEnabled(true)
        .setChunkDeflationLoggingEnabled(false)
        .setAdvancedMonitoringEnabled(false)
        .setAdvancedMonitorVisibleAtStartup(false);
    }

    public static class Handler implements JsonSerializer<AdvancedConfig>, JsonDeserializer<AdvancedConfig> {

        @Override
        public AdvancedConfig deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            AdvancedConfig config = AdvancedConfig.createDefault();
            JsonObject input = json.getAsJsonObject();
            if (input.has("blockDataClassName")) 
                config.setBlockDataClassNameDontThrow(input.get("blockDataClassName").getAsString());
            if (input.has("sunlightDataClassName")) 
                config.setSunlightDataClassNameDontThrow(input.get("sunlightDataClassName").getAsString());
            if (input.has("lightDataClassName")) 
                config.setLightDataClassNameDontThrow(input.get("lightDataClassName").getAsString());
            if (input.has("extraDataClassName")) 
                config.setExtraDataClassNameDontThrow(input.get("extraDataClassName").getAsString());
            if (input.has("chunkDeflationEnabled")) 
                config.setChunkDeflationEnabled(input.get("chunkDeflationEnabled").getAsBoolean());
            if (input.has("chunkDeflationLoggingEnabled")) 
                config.setChunkDeflationLoggingEnabled(input.get("chunkDeflationLoggingEnabled").getAsBoolean());
            if (input.has("advancedMonitoringEnabled"))
                config.setAdvancedMonitoringEnabled(input.get("advancedMonitoringEnabled").getAsBoolean());
            if (input.has("advancedMonitorVisibleAtStartup"))
                config.setAdvancedMonitorVisibleAtStartup(input.get("advancedMonitorVisibleAtStartup").getAsBoolean());
            return config;
        }

        @Override
        public JsonElement serialize(AdvancedConfig src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.addProperty("blockDataClassName", src.blockDataClassName);
            result.addProperty("sunlightDataClassName", src.sunlightDataClassName);
            result.addProperty("lightDataClassName", src.lightDataClassName);
            result.addProperty("extraDataClassName", src.extraDataClassName);
            result.addProperty("chunkDeflationEnabled", src.chunkDeflationEnabled);
            result.addProperty("chunkDeflationLoggingEnabled", src.chunkDeflationLoggingEnabled);
            result.addProperty("advancedMonitoringEnabled", src.advancedMonitoringEnabled);
            result.addProperty("advancedMonitorVisibleAtStartup", src.advancedMonitorVisibleAtStartup);
            return result;
        }
        
    }
    
    public static String[] getTeraArrayFactories() {
        final TeraArrays.Entry[] entries = TeraArrays.getInstance().getCoreArrayEntries();
        final String[] factories = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            factories[i] = entries[i].arrayClassName;
        }
        Arrays.sort(factories);
        return factories;
    }
}
