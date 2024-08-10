/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl.tests;

import codex.renthyl.FrameGraph;
import codex.renthylplus.FrameGraphFactory;
import codex.renthyl.Renthyl;
import codex.renthyl.client.MatParamTargetControl;
import codex.renthyl.modules.Attribute;
import codex.renthyl.modules.ModuleLocator;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
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
        
        Renthyl.initialize(this);
        
        fg = FrameGraphFactory.deferred(assetManager);
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
