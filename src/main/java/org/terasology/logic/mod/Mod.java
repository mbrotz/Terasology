/*
 * Copyright 2012 Benjamin Glatzel <benjamin.glatzel@me.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.logic.mod;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.asset.AssetManager;
import org.terasology.asset.AssetSource;

import com.google.common.base.Preconditions;

/**
 * @author Immortius
 */
public class Mod {
    
    protected static final Logger logger = LoggerFactory.getLogger(Mod.class);
            
    private final ModInfo modInfo;
    private final File modRoot;
    private final AssetSource modSource;
    private boolean enabled;
    private ClassLoader inactiveClassLoader;
    private ClassLoader activeClassLoader;
    private Reflections reflections;

    protected boolean enableMod() {
        AssetManager.getInstance().addAssetSource(modSource);
        return ModDataExtensionRegistry.getInstance().enableModExtensions(this);
    }
    
    protected void disableMod() {
        AssetManager.getInstance().removeAssetSource(modSource);
        ModDataExtensionRegistry.getInstance().disableModExtensions(this);
    }
    
    public Mod(File modRoot, ModInfo info, AssetSource modSource) {
        this.modInfo = Preconditions.checkNotNull(info, "The parameter 'info' must not be null");
        this.modRoot = Preconditions.checkNotNull(modRoot, "The parameter 'modRoot' must not be null");
        this.modSource = Preconditions.checkNotNull(modSource, "The parameter 'modSource' must not be null");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            if (enabled) {
                if (enableMod()) 
                    this.enabled = true;
                else {
                    this.enabled = false;
                    logger.error("Failed activating mod {} ({})", modInfo.getDisplayName(), modInfo.getId());
                    disableMod();
                }
            } else {
                this.enabled = false;
                disableMod();
            }
        }
        return this.enabled;
    }

    public File getModRoot() {
        return modRoot;
    }

    public URL getModClasspathUrl() {
        try {
            if (modRoot.isDirectory()) {
                File classesDir = new File(modRoot, "classes");
                if (classesDir.exists() && classesDir.isDirectory()) {
                    return classesDir.toURI().toURL();
                }
            } else {
                if (modRoot.getAbsolutePath().endsWith(".jar")) {
                    return modRoot.toURI().toURL();
                }
            }
        } catch (MalformedURLException e) {
            return null;
        }
        return null;
    }

    public Reflections getReflections() {
        if (reflections == null) {
            URL url = getModClasspathUrl();
            if (url != null) {
                ConfigurationBuilder configurationBuilder = new ConfigurationBuilder().addUrls(url).setScanners(new TypeAnnotationsScanner(), new SubTypesScanner());
                if (activeClassLoader != null) {
                    configurationBuilder.addClassLoader(activeClassLoader);
                } else {
                    configurationBuilder.addClassLoader(inactiveClassLoader);
                }
                reflections = new Reflections(configurationBuilder);
            }
        }
        return reflections;
    }

    public boolean isCodeMod() {
        return getModClasspathUrl() != null;
    }

    public ClassLoader getActiveClassLoader() {
        return activeClassLoader;
    }

    void setActiveClassLoader(ClassLoader activeClassLoader) {
        this.activeClassLoader = activeClassLoader;
        reflections = null;
    }

    void setInactiveClassLoader(ClassLoader inactiveClassLoader) {
        this.inactiveClassLoader = inactiveClassLoader;
    }

    public ModInfo getModInfo() {
        return modInfo;
    }
}
