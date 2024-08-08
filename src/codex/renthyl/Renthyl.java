/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.material.TechniqueLoader;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.export.binary.BinaryLoader;
import com.jme3.renderer.RenderManager;

/**
 *
 * @author codex
 */
public class Renthyl {
    
    private static Renthyl instance;
    
    public static void initialize(Application app) {
        if (isInitialized()) {
            throw new IllegalStateException(Renthyl.class.getSimpleName()+" has already been initialized.");
        }
        instance = new Renthyl(app);
    }
    
    public static boolean isInitialized() {
        return instance != null;
    }
    
    public static void requireInitialized() {
        if (!isInitialized()) {
            throw new IllegalArgumentException(Renthyl.class.getSimpleName()+" has not been initialized.");
        }
    }
    
    private class AppDestroyListener extends BaseAppState {

        @Override
        protected void initialize(Application app) {}
        @Override
        protected void cleanup(Application app) {
            context.appStopped();
        }
        @Override
        protected void onEnable() {}
        @Override
        protected void onDisable() {}
        
    }
    
    private final FGPipelineContext context;
    
    private Renthyl(Application app) {
        
        RenderManager rm = app.getRenderManager();
        context = new FGPipelineContext(rm);
        rm.registerContext(FrameGraph.CONTEXT_TYPE, context);
        
        app.getStateManager().attach(new AppDestroyListener());
        
        AssetManager assetManager = app.getAssetManager();
        assetManager.registerLoader(BinaryLoader.class, "fg");
        assetManager.registerLoader(TechniqueLoader.class, "fgmt");
        
    }
    
}
