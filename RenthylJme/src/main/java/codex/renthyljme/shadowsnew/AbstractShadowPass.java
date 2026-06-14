package codex.renthyljme.shadowsnew;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyljme.RasterTask;
import codex.renthyljme.definitions.FrameBufferDef;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.render.CameraState;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractShadowPass <T extends Light> extends RasterTask {

    private final TransitiveSocket<Texture2D> sceneDepth = new TransitiveSocket<>(this);
    private final TransitiveSocket<ShadowMask> mask = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new TransitiveSocket<>(this);
    private final ArgumentSocket<T> light = new ArgumentSocket<>(this);
    private final DefinedAllocationSocket<FrameBufferDef, FrameBuffer> frameBuffer;
    private final DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D> shadowMap;
    private final RenderState state = new RenderState();
    private final Material backupMat;

    protected final List<CameraState> cameras = new ArrayList<>();

    public AbstractShadowPass(AssetManager assetManager, ResourceAllocator allocator, int size) {
        addSockets(sceneDepth, mask, occluders, receivers, light);
        frameBuffer = addSocket(new DefinedAllocationSocket<>(this, allocator, new FrameBufferDef()));
        shadowMap = addSocket(new DefinedAllocationSocket<>(this, allocator, TextureDef.texture2D(Image.Format.Depth16)));
        backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }

    @Override
    protected void renderTask() {

        T l = light.acquire();
        Texture2D depth = sceneDepth.acquire();
        GeometryQueue occluderQueue = occluders.acquire();
        GeometryQueue receiverQueue = receivers.acquire();

        context.getCamera().push();
        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        Texture2D sdwMap = shadowMap.acquire();
        frameBuffer.getDef().clearColorTargets();
        frameBuffer.getDef().setDepthTarget(sdwMap);
        FrameBuffer fb = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fb);

        ShadowMask msk = mask.acquireOrThrow("Mask is required.");
        msk.addLight(l);

        occluderQueue.applySettings(context);

        for (CameraState cam : cameras) {
            updateCameraToLight(l, cam);
            context.getCamera().setValue(cam);
            if (receiverQueue.render(context, (c, g) -> {}) > 0) {
                context.clearBuffers(false, true, false);
                occluderQueue.render(context);
                msk.composeLight(depth, sdwMap, cam.getCamera().getViewProjectionMatrix());
            }
        }

        occluderQueue.restoreSettings(context);

        context.getCamera().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();
        context.getFrameBuffer().pop();

    }

    protected abstract void updateCameraToLight(T light, CameraState camera);

    public PointerSocket<Texture2D> getSceneDepth() {
        return sceneDepth;
    }

    public PointerSocket<ShadowMask> getMask() {
        return mask;
    }

    public PointerSocket<GeometryQueue> getOccluders() {
        return occluders;
    }

    public PointerSocket<GeometryQueue> getReceivers() {
        return receivers;
    }

    public ArgumentSocket<T> getLight() {
        return light;
    }

}
