package codex.renthyljme.test.shadows;

import codex.boost.mesh.NormalQuad;
import codex.jmecompute.opengl.GLRenderUtils;
import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyl.tasks.utils.Derivative;
import codex.renthyljme.JmeFrameGraph;
import codex.renthyljme.lights.LightBuffer;
import codex.renthyljme.lights.LightBufferPass;
import codex.renthyljme.lights.LightGatherPass;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyljme.scene.*;
import codex.renthyljme.shadow.ShadowMaskPass;
import codex.renthyljme.shadow.ShadowManager;
import codex.renthyljme.shadow.ShadowMap;
import codex.renthyljme.utils.InputToggledMux;
import codex.renthyljme.utils.MaterialUtils;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.util.mikktspace.MikktspaceTangentGenerator;

import java.util.Collection;

public class TestShadows extends SimpleApplication {

    public static void main(String[] args) {
        TestShadows app = new TestShadows();
        AppSettings settings = new AppSettings(true);
        settings.setWidth(800);
        settings.setHeight(800);
        settings.setRenderer(AppSettings.LWJGL_OPENGL45);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {

        GLRenderUtils.initialize(this);
        viewPort.setBackgroundColor(ColorRGBA.Blue);
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        flyCam.setMoveSpeed(10);

        createShape(new NormalQuad(Vector3f.UNIT_Y, Vector3f.UNIT_Z, 10f, 10f, 0.5f, 0.5f), ColorRGBA.White, 0.9f, 0.1f);
        createShape(new Box(1f, 1f, 1f), ColorRGBA.White, 0f, 1f).setLocalTranslation(0f, 2f, 0f);

        DirectionalLight dl = new DirectionalLight();
        dl.setDirection(new Vector3f(0.4f, -1f, 0.8f));
        dl.setColor(ColorRGBA.White.mult(0.01f));
        rootNode.addLight(dl);

        PointLight pl = new PointLight();
        pl.setPosition(new Vector3f(3f, 4f, -4f));
        pl.setRadius(20f);
        pl.setColor(ColorRGBA.Red);
        rootNode.addLight(pl);

        SpotLight sl = new SpotLight();
        sl.setPosition(new Vector3f(-5f, 6f, 5f));
        sl.setDirection(new Vector3f(1f, -1f, -1f));
        sl.setSpotOuterAngle(FastMath.HALF_PI * 0.5f);
        sl.setSpotInnerAngle(FastMath.HALF_PI * 0.25f);
        sl.setSpotRange(20f);
        sl.setColor(ColorRGBA.Green);
        rootNode.addLight(sl);

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        JmeFrameGraph fg = new JmeFrameGraph(assetManager);
        viewPort.setPipeline(fg);
        fg.addTask(new ControlRenderPass());

        ShadowManager shadows = new ShadowManager(assetManager, allocator);
        shadows.addDirectionalLightSource(new Attribute<>(dl), 1024, 1);
        shadows.addPointLightSource(new Attribute<>(pl), 1024);
        shadows.addSpotLightSource(new Attribute<>(sl), 1024);
        ShadowMaskPass shadowComposer = new ShadowMaskPass(assetManager, allocator);
        shadowComposer.getShadowMaps().addCollectionSource(shadows.getShadowMaps());

        SceneEnqueuePass enqueue = SceneEnqueuePass.withLegacyQueues();
        shadows.getGeometry().addMapSource(enqueue.getQueues());

        GeometryDepthPass depth = new GeometryDepthPass(allocator);
        depth.getGeometry().addMapSource(enqueue.getQueues());
        shadowComposer.getSceneDepth().setUpstream(depth.getDepth());

        LightGatherPass lightGather = new LightGatherPass();
        LightBufferPass lightBuffer = new LightBufferPass(allocator);
        lightBuffer.getLights().addCollectionSource(lightGather.getLights());
        lightBuffer.getLightShadowMapping().setUpstream(shadowComposer.getLightShadowMapping());

        GeometryPass geometry = new GeometryPass(allocator);
        geometry.getGeometry().addMapSource(enqueue.getQueues());
        geometry.getParameter("ShadowMask").setUpstream(shadowComposer.getShadowMask());
        geometry.getParameter("LightData").setUpstream(new Derivative<LightBuffer, float[]>() {
            private float[] array;
            @Override
            public float[] apply(LightBuffer buf) {
                return (array = buf.copyDataTo(array));
            }
        }.setUpstream(lightBuffer.getLightData()));
        geometry.getParameter("NumLights").setUpstream(new Derivative<Collection<Light>, Integer>() {
            @Override
            public Integer apply(Collection<Light> lights) {
                return lights.size() * 12;
            }
        }.setUpstream(lightGather.getLights()));
        geometry.getColorDef().setFormat(Image.Format.RGBA32F);

        InputToggledMux<Texture2D> outChannel = new InputToggledMux<>();
        outChannel.addUpstream(geometry.getOutColor());
        //outChannel.addUpstream(geometry.getOutDepth());
        outChannel.addUpstream(depth.getDepth());
        outChannel.addUpstream(shadowComposer.getShadowMask());
        outChannel.addUpstream(new Derivative<Collection<ShadowMap>, Texture2D>() {
            @Override
            public Texture2D apply(Collection<ShadowMap> shadowMaps) {
                return shadowMaps.stream().findFirst().orElseThrow().getMap();
            }
        }.setUpstream(shadows.getShadowMaps()));
        inputManager.addMapping("toggle_channel", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addListener(outChannel, "toggle_channel");

        OutputPass out = fg.addTask(new OutputPass());
        out.getColor().setUpstream(outChannel);

    }

    private Geometry createShape(Mesh mesh, ColorRGBA color, float metallic, float roughness) {
        Geometry g = new Geometry("shape", mesh);
        Material floorMat = new Material(assetManager, MaterialUtils.RENTHYL_PBR_LIGHTING);
        floorMat.setColor("BaseColor", color);
        floorMat.setFloat("Metallic", metallic);
        floorMat.setFloat("Roughness", roughness);
        g.setMaterial(floorMat);
        MikktspaceTangentGenerator.generate(g);
        rootNode.attachChild(g);
        return g;
    }

}
