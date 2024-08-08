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

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.ChaseCamera;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 *
 * @author codex
 */
public abstract class TestApplication extends SimpleApplication {

    protected final Vector2f windowSize = new Vector2f();
    
    public static void main(String[] args) {
        TestApplication app = new TestAppImpl();
        app.applySettings();
        app.start();
    }
    
    protected AppSettings applySettings() {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(756);
        settings.setHeight(756);
        setSettings(settings);
        return settings;
    }
    protected void setupCam(Spatial target) {
        ChaseCamera chaser = new ChaseCamera(cam, target, inputManager);
        chaser.setDragToRotate(true);
        chaser.setMinVerticalRotation(-FastMath.HALF_PI);
        chaser.setMaxDistance(1000);
        chaser.setSmoothMotion(true);
        chaser.setRotationSensitivity(10);
        chaser.setZoomSensitivity(5);
        flyCam.setEnabled(false);
    }
    protected void setupLight() {
        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1, -1, -1));
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);
        rootNode.addLight(new AmbientLight(ColorRGBA.White.mult(0.1f)));
        rootNode.addControl(new EnvironmentProbeControl(assetManager, 256));
    }
    protected Spatial loadSky() {
        Spatial sky = SkyFactory.createSky(assetManager, "Textures/Sky/Path.hdr", SkyFactory.EnvMapType.EquirectMap);
        rootNode.attachChild(sky);
        EnvironmentProbeControl.tagGlobal(sky);
        return sky;
    }
    protected Spatial loadTank() {
        assetManager.registerLoader(KTXLoader.class, "ktx");
        viewPort.setBackgroundColor(ColorRGBA.White);
        Spatial model = assetManager.loadModel("Models/Tank/tank.j3o");
        model.setName("Tank");
        MikktspaceTangentGenerator.generate(model);
        rootNode.attachChild(model);
        Material pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);
        return model;
    }
    protected BitmapText loadText(String text, float x, float y, float fontSize) {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText bitmap = new BitmapText(font);
        bitmap.setSize(fontSize <= 0 ? font.getCharSet().getRenderedSize() : fontSize);
        bitmap.setText(text);
        bitmap.setLocalTranslation(x, y, 0);
        guiNode.attachChild(bitmap);
        return bitmap;
    }
    protected Geometry loadTextureViewer(float x, float y, float w, float h) {
        Geometry g = new Geometry("TextureViewer", new Quad(w, h));
        g.setLocalTranslation(x, y, 0);
        Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        g.setMaterial(m);
        guiNode.attachChild(g);
        return g;
    }
    
    protected void setupAll() {
        setupCam(loadTank());
        setupLight();
        loadSky();
    }
    
    @Override
    public void simpleInitApp() {
        windowSize.x = context.getSettings().getWidth();
        windowSize.y = context.getSettings().getHeight();
        testInitApp();
    }
    
    protected abstract void testInitApp();
    
    private static class TestAppImpl extends TestApplication {

        @Override
        public void testInitApp() {
            setupAll();
        }
        
    }
    
}
