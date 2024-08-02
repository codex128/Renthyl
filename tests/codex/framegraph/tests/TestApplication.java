/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.tests;

import com.jme3.app.SimpleApplication;
import com.jme3.environment.EnvironmentProbeControl;
import com.jme3.input.ChaseCamera;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.texture.plugins.ktx.KTXLoader;
import com.jme3.util.SkyFactory;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

/**
 *
 * @author codex
 */
public abstract class TestApplication extends SimpleApplication {

    public static void main(String[] args) {
        TestApplication app = new TestAppImpl();
        app.applySettings();
        app.start();
    }
    
    protected void applySettings() {
        AppSettings settings = new AppSettings(true);
        settings.setWidth(756);
        settings.setHeight(756);
        setSettings(settings);
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
        MikktspaceTangentGenerator.generate(model);
        rootNode.attachChild(model);
        Material pbrMat = assetManager.loadMaterial("Models/Tank/tank.j3m");
        model.setMaterial(pbrMat);
        return model;
    }
    
    protected void setupAll() {
        setupCam(loadTank());
        setupLight();
        loadSky();
    }
    
    private static class TestAppImpl extends TestApplication {

        @Override
        public void simpleInitApp() {
            setupAll();
        }
        
    }
    
}
