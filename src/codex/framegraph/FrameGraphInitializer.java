/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph;

import codex.framegraph.material.TechniqueLoader;
import com.jme3.asset.AssetManager;

/**
 *
 * @author codex
 */
public class FrameGraphInitializer {
    
    private boolean initialized = false;
    
    public void initialize(AssetManager assetManager) {
        
        if (initialized) return;
        
        // for loading FGMT (Frame Graph Material Technique) files
        assetManager.registerLoader(TechniqueLoader.class, "fgmt");
        
        initialized = true;
        
    }
    
}
