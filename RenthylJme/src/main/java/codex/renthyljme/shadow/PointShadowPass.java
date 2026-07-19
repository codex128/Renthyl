/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.jmecompute.WorkSize;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyljme.definitions.FrameBufferDef;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyljme.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class PointShadowPass extends RasterTask implements Occlusion<PointLight> {

    private static final Quaternion[] ROTATIONS = {
        new Quaternion().lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y),
        new Quaternion().lookAt(Vector3f.UNIT_Y, Vector3f.UNIT_Z),
        new Quaternion().lookAt(Vector3f.UNIT_Z, Vector3f.UNIT_Y),
        new Quaternion().lookAt(Vector3f.UNIT_X.negate(), Vector3f.UNIT_Y),
        new Quaternion().lookAt(Vector3f.UNIT_Y.negate(), Vector3f.UNIT_Z),
        new Quaternion().lookAt(Vector3f.UNIT_Z.negate(), Vector3f.UNIT_Y),
    };

    private final ArgumentSocket<PointLight> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<ShadowMask> mask = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final TransitiveSocket<GeometryQueue> receivers = new OptionalSocket<>(this, false);
    private final DefinedAllocationSocket<FrameBufferDef, FrameBuffer> frameBuffer;
    private final DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D> shadowMap;
    private final CameraState[] cameras = new CameraState[ROTATIONS.length];
    private final Material backupMat;
    private final RenderState state = new RenderState();
    
    public PointShadowPass(AssetManager assetManager, ResourceAllocator allocator, int size) {
        addSockets(light, mask, occluders, receivers);
        for (int i = 0; i < ROTATIONS.length; i++) {
            Camera c = (cameras[i] = new CameraState(new Camera(size, size), false)).getCamera();
            c.setRotation(ROTATIONS[i]);
            c.setFrustumPerspective(90f, 1f, 0.3f, 2f);
        }
        frameBuffer = addSocket(new DefinedAllocationSocket<>(this, allocator, new FrameBufferDef()));
        shadowMap = addSocket(new DefinedAllocationSocket<>(this, allocator, TextureDef.texture2D(Image.Format.Depth16)));
        shadowMap.getDef().setSize(size, size);
        backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }

    @Override
    protected void renderTask() {

        ShadowMask maskMap = mask.acquireOrThrow();
        int maskIndex = maskMap.getNextMaskIndex();
        if (maskIndex < 0) return;

        PointLight pl = light.acquireOrThrow("Light required.");

        context.getCamera().push();
        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        Texture2D shadow = shadowMap.acquire();
        frameBuffer.getDef().clearColorTargets();
        frameBuffer.getDef().setDepthTarget(shadow);
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);

        GeometryQueue occluderQueue = occluders.acquireOrThrow("Occluder queue required.");
        BoundingBox camBounds = new BoundingBox();

        for (CameraState cam : cameras) {

            if (!cam.getCamera().getLocation().equals(pl.getPosition()) || cam.getCamera().getFrustumFar() != pl.getRadius()) {
                cam.getCamera().setLocation(pl.getPosition());
                cam.getCamera().setFrustumFar(pl.getRadius());
                cam.getCamera().update();
                cam.getCamera().updateViewProjection();
            }

            Occlusion.computeShadowCameraBounds(cam.getCamera(), camBounds);
            if (maskMap.getCamera().contains(camBounds) == Camera.FrustumIntersect.Outside) {
                continue;
            }

            context.getCamera().setValue(cam);

            context.clearBuffers(false, true, false);
            occluderQueue.render(context);
            maskMap.compose(shadow, cam.getCamera().getViewProjectionMatrix(), pl, maskIndex);

        }

        context.getFrameBuffer().pop();
        context.getCamera().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();

    }

    @Override
    public ArgumentSocket<PointLight> getLight() {
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
        return mask;
    }

}
