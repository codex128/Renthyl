/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.tests;

import codex.framegraph.FrameGraph;
import codex.framegraph.FrameGraphFactory;
import codex.framegraph.modules.deferred.GBufferPass;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;

/**
 *
 * @author codex
 */
public class TestGBuffer extends TestApplication {

    public static void main(String[] args) {
        TestGBuffer app = new TestGBuffer();
        app.applySettings();
        app.start();
    }
    
    @Override
    protected void testInitApp() {
        
        FrameGraph.initialize(this);
        FrameGraph fg = FrameGraphFactory.Test.testGBuffer(assetManager);
        fg.setSetting("GBuffer", 0);
        viewPort.setPipeline(fg);
        
        GBufferPass.adaptAllMaterials(assetManager);
        
        setupAll();
        
        ActionListener action = (String name, boolean isPressed, float tpf) -> {
            if (isPressed) {
                int n = fg.getSetting("GBuffer");
                if (name.equals("up")) {
                    fg.setSetting("GBuffer", wrap(n+1, 0, 4));
                } else if (name.equals("down")) {
                    fg.setSetting("GBuffer", wrap(n-1, 0, 4));
                }
            }
        };
        
        inputManager.addMapping("up", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("down", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addListener(action, "up", "down");
        
    }
    
    private static int wrap(int value, int min, int max) {
        if (value < min) return max-min+value+1;
        else if (value > max) return min+value-max-1;
        else return value;
    }
    
}
