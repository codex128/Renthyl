/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.tests;

import codex.framegraph.FrameGraph;
import codex.framegraph.FrameGraphFactory;

/**
 *
 * @author codex
 */
public class TestForward extends TestApplication {

    public static void main(String[] args) {
        TestForward forward = new TestForward();
        forward.applySettings();
        forward.start();
    }
    
    @Override
    public void testInitApp() {
        
        FrameGraph.initialize(this);
        
        FrameGraph fg = FrameGraphFactory.forward(assetManager);
        viewPort.setPipeline(fg);
        
        setupAll();
        
    }
    
}
