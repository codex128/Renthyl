/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.asset;

import codex.framegraph.export.ModuleGraphData;
import com.jme3.asset.AssetKey;
import com.jme3.asset.cache.AssetCache;

/**
 *
 * @author codex
 */
public class ModuleGraphKey extends AssetKey<ModuleGraphData> {
    
    public ModuleGraphKey(String name) {
        super(name);
    }
    public ModuleGraphKey() {
        super();
    }
    
    @Override
    public Class<? extends AssetCache> getCacheType() {
        return null;
    }
    
}
