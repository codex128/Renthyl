/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.modules.geometry.OutputGeometryPass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
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
    
    /**
     * Initializes Renthyl.
     * <p>
     * This should be called before using Renthyl in a JMonkeyEngine application.
     * 
     * @param app 
     */
    public static void initialize(Application app) {
        if (isInitialized()) {
            throw new IllegalStateException(Renthyl.class.getSimpleName()+" has already been initialized.");
        }
        instance = new Renthyl(app);
    }
    
    /**
     * Returns true if Renthyl is initialized.
     * 
     * @return 
     */
    public static boolean isInitialized() {
        return instance != null;
    }
    
    /**
     * Throws an exception if Renthyl is not initialized.
     */
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
            context.applicationStopped();
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
        
    }
    
    /**
     * Creates a simple forward-style FrameGraph.
     * 
     * @param assetManager
     * @return 
     */
    public static FrameGraph forward(AssetManager assetManager) {
        
        FrameGraph fg = new FrameGraph(assetManager);
        fg.setName("Forward");
        
        SceneEnqueuePass enqueue = fg.add(new SceneEnqueuePass(true, true));
        QueueMergePass merge = fg.add(new QueueMergePass(5));
        OutputGeometryPass out = fg.add(new OutputGeometryPass());
        
        merge.makeInput(enqueue, "Opaque", "Queues[0]");
        merge.makeInput(enqueue, "Sky", "Queues[1]");
        merge.makeInput(enqueue, "Transparent", "Queues[2]");
        merge.makeInput(enqueue, "Gui", "Queues[3]");
        merge.makeInput(enqueue, "Translucent", "Queues[4]");
        
        out.makeInput(merge, "Result", "Geometry");
        
        return fg;
        
    }
    
}
