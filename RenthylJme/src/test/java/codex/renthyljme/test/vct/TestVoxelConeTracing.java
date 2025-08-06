package codex.renthyljme.test.vct;

import codex.boost.mesh.NormalQuad;
import codex.jmecompute.opengl.GLRenderUtils;
import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyljme.JmeFrameGraph;
import codex.renthyljme.gi.vct.VoxelConeTracer;
import codex.renthyljme.lights.LightBufferPass;
import codex.renthyljme.lights.LightGatherPass;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyljme.scene.GeometryDepthPass;
import codex.renthyljme.scene.OutputPass;
import codex.renthyljme.scene.SceneEnqueuePass;
import codex.renthyljme.shadow.ShadowMaskPass;
import codex.renthyljme.shadow.ShadowManager;
import codex.renthyljme.utils.InputToggledMux;
import codex.renthyljme.utils.MaterialUtils;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;

public class TestVoxelConeTracing extends SimpleApplication {

    public static void main(String[] args) {
        TestVoxelConeTracing app = new TestVoxelConeTracing();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(800);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        DetailedProfilerState profiler = new DetailedProfilerState();
        stateManager.attach(profiler);

        createGeometry(0f, 0f, 0f, new Box(1f, 1f, 1f), ColorRGBA.Blue);
        createGeometry(0f, -3f, 0f, new NormalQuad(Vector3f.UNIT_Y, Vector3f.UNIT_Z, 20f, 20f, 0.5f, 0.5f), ColorRGBA.Gray);

        DirectionalLight dl = new DirectionalLight(new Vector3f(1f, -1f, 1f));
        rootNode.addLight(dl);

        GLRenderUtils.initialize(this);
        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        JmeFrameGraph fg = new JmeFrameGraph(assetManager);
        //viewPort.setPipeline(fg);

        viewPort.setBackgroundColor(ColorRGBA.Red);
        flyCam.setMoveSpeed(10f);
        flyCam.setDragToRotate(true);

        SceneEnqueuePass queues = SceneEnqueuePass.withLegacyQueues();
        GeometryDepthPass depth = new GeometryDepthPass(allocator);
        depth.getGeometry().addMapSource(queues.getQueues());

        ShadowManager shadows = new ShadowManager(assetManager, allocator);
        shadows.addDirectionalLightSource(new Attribute<>(dl), 1024, 1);
        ShadowMaskPass mask = new ShadowMaskPass(assetManager, allocator);
        mask.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        mask.getSceneDepth().setUpstream(depth.getDepth());

        LightGatherPass lightGather = new LightGatherPass();
        LightBufferPass lightBuffer = new LightBufferPass(allocator);
        lightBuffer.getLights().addCollectionSource(lightGather.getLights());
        lightBuffer.getLightShadowMapping().setUpstream(mask.getLightShadowMapping());

        VoxelConeTracer vct = new VoxelConeTracer(assetManager, allocator);
        vct.getGridSize().setValue(256);
        vct.getGeometry().addMapSource(queues.getQueues());
        vct.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        vct.getLightBuffer().setUpstream(lightBuffer.getLightData());
        vct.getLightContribution().setUpstream(mask.getShadowMask());

        InputToggledMux<Texture2D> outChannel = new InputToggledMux<>();
        outChannel.addUpstream(vct.getResult());
        outChannel.addUpstream(depth.getDepth());
        inputManager.addMapping("toggle_output", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(outChannel, "toggle_output");

        OutputPass out = fg.addTask(new OutputPass());
        out.getColor().setUpstream(outChannel);
        out.getDepth().setUpstream(depth.getDepth());

        //vct.getResultSelector().setValue(1);

    }

    @Override
    public void simpleUpdate(float tpf) {
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
    }

    private Geometry createGeometry(float x, float y, float z, Mesh mesh, ColorRGBA color) {
        Geometry g = new Geometry("geometry", mesh);
        g.setLocalTranslation(x, y, z);
        Material m = new Material(assetManager, MaterialUtils.JME_PBR_LIGHTING);
        m.setColor("BaseColor", color);
        m.setFloat("Metallic", 0f);
        m.setFloat("Roughness", 0.8f);
        g.setMaterial(m);
        rootNode.attachChild(g);
        return g;
    }

}
