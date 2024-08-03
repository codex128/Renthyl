/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.tests;

import codex.framegraph.FrameGraph;
import codex.framegraph.FrameGraphFactory;
import codex.framegraph.client.MatParamTargetControl;
import codex.framegraph.modules.Attribute;
import codex.framegraph.modules.ModuleLocator;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.shader.VarType;

/**
 *
 * @author codex
 */
public class TestDeferred extends TestApplication implements ActionListener {
    
    private FrameGraph fg;
    private BitmapText hud;
    
    public static void main(String[] args){
        TestDeferred app = new TestDeferred();
        app.applySettings();
        app.start();
    }
    
    @Override
    public void testInitApp() {
        
        FrameGraph.initialize(this);
        
        fg = FrameGraphFactory.deferred(assetManager, false, false);
        viewPort.setPipeline(fg);
        
        setupAll();
        hud = loadText("", 5, windowSize.y-5, -1);
        reloadHud();
        
        Spatial tank = loadTank();
        tank.setLocalTranslation(20, 0, 0);
        tank.setQueueBucket(RenderQueue.Bucket.Transparent);
        
        int viewerSize = 200;
        Geometry viewer = loadTextureViewer(windowSize.x-viewerSize, 0, viewerSize, viewerSize);
        MatParamTargetControl viewerTarget = new MatParamTargetControl("ColorMap", VarType.Texture2D);
        fg.get(ModuleLocator.by(Attribute.class, "GBufferDebug")).addTarget(viewerTarget);
        viewer.addControl(viewerTarget);
        
        fg.setSetting("GBufferDebug", 0);
        
        inputManager.addMapping("UseLightTextures", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("UseLightTiling", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("gbufUp", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addListener(this, "UseLightTextures", "UseLightTiling", "gbufUp");
        
    }
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (name.equals("UseLightTextures") && isPressed) {
            fg.toggleFeature("UseLightTextures");
            reloadHud();
        } else if (name.equals("UseLightTiling") && isPressed) {
            fg.toggleFeature("UseLightTiling");
            reloadHud();
        } else if (name.equals("gbufUp") && isPressed) {
            int n = fg.getSetting("GBufferDebug");
            fg.setSetting("GBufferDebug", wrap(n+1, 0, 4));
        }
    }
    
    private void reloadHud() {
        hud.setText("UseLightTextures: "+fg.isFeatureEnabled("UseLightTextures")
                + "\nUseLightTiles: "+fg.isFeatureEnabled("UseLightTiling"));
    }
    
    private static int wrap(int value, int min, int max) {
        if (value < min) return max-min+value+1;
        else if (value > max) return min+value-max-1;
        else return value;
    }
    
}
