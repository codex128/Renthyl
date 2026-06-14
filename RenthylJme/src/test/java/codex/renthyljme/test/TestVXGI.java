package codex.renthyljme.test;

import codex.jmecompute.opengl.GLRenderUtils;
import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.utils.MapToList;
import codex.renthyljme.JmeFrameGraph;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.gi.vct.VoxelConeTracer;
import codex.renthyljme.lights.LightBufferPass;
import codex.renthyljme.lights.LightGatherPass;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyljme.scene.GeometryDepthPass;
import codex.renthyljme.scene.OutputPass;
import codex.renthyljme.scene.SceneEnqueuePass;
import codex.renthyljme.shadow.ShadowComposerPass;
import codex.renthyljme.shadow.ShadowManager;
import com.jme3.app.DetailedProfilerState;
import com.jme3.app.SimpleApplication;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;

public class TestVXGI extends SimpleApplication {

    public static void main(String[] args) {
        TestVXGI app = new TestVXGI();
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        settings.setWidth(800);
        settings.setHeight(800);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        GLRenderUtils.initialize(this);

        DetailedProfilerState profiler = new DetailedProfilerState();
        stateManager.attach(profiler);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(-1f, -1f, 1f));
        dl.setColor(ColorRGBA.White);
        rootNode.addLight(dl);

        Geometry g = new Geometry("box", new Box(1f, 1f, 1f));
        Material m = new Material(assetManager, "RenthylJme/MatDefs/PBRLighting.j3md");
        m.setColor("BaseColor", ColorRGBA.White);

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        JmeFrameGraph fg = new JmeFrameGraph(assetManager);
        viewPort.setPipeline(fg);

        SceneEnqueuePass scene = SceneEnqueuePass.withLegacyQueues();
        MapToList<String, GeometryQueue> geomToList = new MapToList<>(new String[] {"Opaque", "Sky", "Transparent", "Gui", "Translucent"});
        geomToList.getMap().setUpstream(scene.getQueues());

        GeometryDepthPass depth = new GeometryDepthPass(allocator);
        depth.getGeometry().addCollectionSource(geomToList.getList());

        ShadowManager shadows = new ShadowManager(assetManager, allocator);
        shadows.addDirectionalLightSource(new Attribute<>(dl), 1024, 1);
        ShadowComposerPass composer = new ShadowComposerPass(assetManager, allocator);
        composer.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        composer.getSceneDepth().setUpstream(depth.getDepth());

        LightGatherPass lightGather = new LightGatherPass();
        LightBufferPass lightBuffer = new LightBufferPass(allocator);
        lightBuffer.getLights().addCollectionSource(lightGather.getLights());
        lightBuffer.getLightShadowMapping().setUpstream(composer.getLightShadowMapping());

        VoxelConeTracer vct = new VoxelConeTracer(assetManager, allocator);
        vct.getGridSize().setValue(512);
        vct.getLightContribution().setUpstream(composer.getShadowMask());
        vct.getLightBuffer().setUpstream(lightBuffer.getLightData());
        vct.getGeometry().addCollectionSource(geomToList.getList());
        vct.getShadowMaps().addCollectionSource(shadows.getShadowMaps());
        vct.getLightContribution().setUpstream(composer.getShadowMask());
        vct.getLightBuffer().setUpstream(lightBuffer.getLightData());

        OutputPass out = fg.addTask(new OutputPass());
        out.getColor().setUpstream(vct.getResult());
        out.getDepth().setUpstream(depth.getDepth());

        flyCam.setDragToRotate(true);

    }

}
