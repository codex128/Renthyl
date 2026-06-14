package codex.renthyljme.test.shadows;

import codex.renthyl.sockets.SynchronizedArgumentSocket;
import codex.renthyl.tasks.attributes.GlobalAccessor;
import codex.renthyl.tasks.utils.SocketFrame;
import codex.renthyljme.FrameGraphContext;
import codex.renthyljme.JmeFrameGraph;
import codex.renthyljme.resources.ResourceAllocationState;
import codex.renthyljme.scene.GeometryPass;
import codex.renthyljme.shadowsnew.PointShadowPass;
import codex.renthyljme.shadowsnew.ShadowMask;
import codex.renthyljme.shadowsnew.ShadowMaskFactory;
import com.jme3.app.SimpleApplication;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Texture2D;

/**
 * This is testing an interesting concept to make many shadows more
 * efficient. Instead of rendering all shadow maps and then computing
 * the shadow mask, each shadow map is immediately written into the
 * mask, and then the shadow map can be reused for the next shadow
 * map render.
 *
 * <p>If you have 3 shadow-casting PointLights, that would normally require
 * 18 shadow maps. Assuming they are all the same resolution, this method
 * would require only 1.</p>
 *
 * @author codex
 */
public class TestEfficientShadows extends SimpleApplication {

    public static void main(String[] args) {
        TestEfficientShadows app = new TestEfficientShadows();
        app.start();
    }

    @Override
    public void simpleInitApp() {

        ResourceAllocationState allocator = new ResourceAllocationState();
        stateManager.attach(allocator);

        JmeFrameGraph fg = new JmeFrameGraph(assetManager);
        viewPort.setPipeline(fg);

        PointLight p1 = new PointLight();
        p1.setPosition(new Vector3f(0f, 0f, 0f));
        p1.setRadius(20f);
        p1.setColor(ColorRGBA.White);
        rootNode.addLight(p1);

        GlobalAccessor<Camera> vpCam = new GlobalAccessor<>(FrameGraphContext.CAMERA_GLOBAL);

        ShadowMaskFactory maskFactory = new ShadowMaskFactory(assetManager, allocator);
        maskFactory.getCamera().setUpstream(vpCam);
        SocketFrame<SynchronizedArgumentSocket<ShadowMask>> maskSync = new SocketFrame<>(SynchronizedArgumentSocket::new);
        maskSync.get().setUpstream(maskFactory.getMask());

        PointShadowPass point1 = new PointShadowPass(assetManager, allocator, 1024);
        point1.getLight().setValue(p1);
        point1.getMask().setUpstream(maskSync.get());

        GeometryPass geometry = new GeometryPass(allocator);
        geometry.getParameter("ShadowMask").setUpstream(point1.getMask());

    }

}
