/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyljme.definitions.FrameBufferDef;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyljme.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class SpotShadowPass extends RasterTask implements Occlusion<SpotLight> {

    private final ArgumentSocket<SpotLight> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<ShadowMask> shadowMask = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new OptionalSocket<>(this, false);
    private final ArgumentSocket<Float> nearFrustum = new ArgumentSocket<>(this, 1f);
    private final DefinedAllocationSocket<FrameBufferDef, FrameBuffer> frameBuffer;
    private final DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D> shadowMap;
    private final CameraState camera;
    private final Material backupMat;
    private final RenderState state = new RenderState();
    
    public SpotShadowPass(AssetManager assetManager, ResourceAllocator allocator, int size) {
        addSockets(light, shadowMask, occluders, receivers, nearFrustum);
        frameBuffer = addSocket(new DefinedAllocationSocket<>(this, allocator, new FrameBufferDef()));
        shadowMap = addSocket(new DefinedAllocationSocket<>(this, allocator, TextureDef.texture2D(Image.Format.Depth16)));
        camera = new CameraState(new Camera(size, size), false);
        backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }

    @Override
    protected void renderTask() {

        ShadowMask mask = shadowMask.acquire();
        int maskIndex = mask.getNextMaskIndex();
        if (maskIndex < 0) return;

        SpotLight sl = light.acquireOrThrow("Light required");

        camera.getCamera().setLocation(sl.getPosition());
        camera.getCamera().lookAtDirection(sl.getDirection(), camera.getCamera().getUp());
        camera.getCamera().setFrustumPerspective(sl.getSpotOuterAngle() * FastMath.RAD_TO_DEG * 2f, 1f, nearFrustum.acquire(0.1f), sl.getSpotRange());
        camera.getCamera().update();
        camera.getCamera().updateViewProjection();
        context.getCamera().pushValue(camera);

        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        Texture2D map = shadowMap.acquire();
        frameBuffer.getDef().clearColorTargets();
        frameBuffer.getDef().setDepthTarget(map);
        context.getFrameBuffer().pushValue(frameBuffer.acquire());
        context.clearBuffers();

        occluders.acquireOrThrow("Receiver queue required").render(context);

        mask.compose(map, camera.getCamera().getViewProjectionMatrix(), sl, maskIndex);

        context.getCamera().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();
        context.getFrameBuffer().pop();

    }

    @Override
    public ArgumentSocket<SpotLight> getLight() {
        return light;
    }

    @Override
    public PointerSocket<GeometryQueue> getOccluders() {
        return occluders;
    }

    @Override
    public PointerSocket<GeometryQueue> getReceivers() {
        return receivers;
    }

    @Override
    public PointerSocket<ShadowMask> getShadowMask() {
        return shadowMask;
    }

    public ArgumentSocket<Float> getNearFrustum() {
        return nearFrustum;
    }

}
