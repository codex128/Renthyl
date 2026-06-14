/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.renthyl.sockets.*;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyljme.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;

import java.util.Collection;

/**
 *
 * @author codex
 */
public class DirectionalShadowPass extends RasterTask implements Occlusion<DirectionalLight> {

    private static final float MIN_FRUSTUM = .5f;

    private final int baseMapSize;
    private final ArgumentSocket<DirectionalLight> light = new ArgumentSocket<>(this);
    private final TransitiveSocket<GeometryQueue> occluders = new TransitiveSocket<>(this);
    private final OptionalSocket<GeometryQueue> receivers = new OptionalSocket<>(this, false);
    private final ArgumentSocket<Float> maxDistance = new ArgumentSocket<>(this, 1000f);
    private final SocketList<ShadowMapSocket, ShadowMap> shadowMaps = new SocketList<>(this);
    private final CameraState camera = new CameraState(new Camera(1024, 1024), false);
    private final Material backupMat;
    private final RenderState state = new RenderState();
    
    public DirectionalShadowPass(AssetManager assetManager, ResourceAllocator allocator, int shadowMapSize, int cascades) {
        if ((shadowMapSize >> (cascades - 1)) <= 1) {
            throw new IllegalArgumentException("Base shadow map size must be greater than 2^cascades.");
        }
        this.baseMapSize = shadowMapSize;
        this.backupMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        addSockets(light, occluders, receivers, maxDistance, shadowMaps);
        for (int i = 0; i < cascades; i++) {
            shadowMaps.add(new ShadowMapSocket(this, allocator));
        }
        camera.getCamera().setParallelProjection(true);
        state.setColorWrite(false);
        state.setDepthWrite(true);
        state.setDepthTest(true);
    }
    
    @Override
    protected void renderTask() {

        // setup context settings
        context.getCamera().push();
        context.getFrameBuffer().push();
        context.getForcedTechnique().pushValue("PreShadow");
        context.getForcedMaterial().pushValue(backupMat);
        context.getForcedState().pushValue(state);

        DirectionalLight dl = light.acquireOrThrow("Light required.");

        // calculate frustum
        float nearFrustum = MIN_FRUSTUM;
        float farFrustum = maxDistance.acquireOrThrow("Maximum render distance required.");
        GeometryQueue occluderQueue = occluders.acquireOrThrow("Occluder geometry required.");
        BoundingBox occluderBB = new BoundingBox();
        for (Geometry g : occluderQueue) {
            g.updateGeometricState();
            occluderBB.mergeLocal(g.getWorldBound());
        }
        float radius = positionCamera(dl, occluderBB, nearFrustum, farFrustum);
        farFrustum = Math.min(farFrustum, nearFrustum + radius * 2f);

        int size = baseMapSize;
        float cascadeLength = (farFrustum - nearFrustum) / shadowMaps.size();

        for (ShadowMapSocket socket : shadowMaps) {
            farFrustum = nearFrustum + cascadeLength;

            // configure camera
            camera.resize(size, size, false);
            camera.getCamera().setFrustumNear(nearFrustum);
            camera.getCamera().setFrustumFar(farFrustum);
            camera.getCamera().update();
            camera.getCamera().updateViewProjection();
            context.getCamera().setValue(camera);

            // configure shadow map
            socket.setSize(size, size);
            ShadowMap map = socket.getShadowMap().acquire();
            map.setLight(dl);
            map.setProjection(camera.getCamera().getViewProjectionMatrix());
            map.setRange(nearFrustum, farFrustum);

            // configure framebuffer
            socket.setTargetDepth(map.getMap());
            FrameBuffer fbo = socket.getFrameBuffer().acquire();
            context.getFrameBuffer().setValue(fbo);
            context.clearBuffers();

            // render
            occluderQueue.render(context);

            nearFrustum = farFrustum;
            size = size >> 1;
        }

        // restore context settings
        context.getCamera().pop();
        context.getFrameBuffer().pop();
        context.getForcedTechnique().pop();
        context.getForcedMaterial().pop();
        context.getForcedState().pop();

    }

    private float positionCamera(DirectionalLight dl, BoundingBox include, float near, float far) {
        float radius = include.getMax(null).subtractLocal(include.getCenter()).length();
        camera.getCamera().setLocation(include.getCenter().subtract(dl.getDirection().mult(radius + near)));
        camera.getCamera().lookAtDirection(dl.getDirection(), camera.getCamera().getUp());
        camera.getCamera().setFrustum(near, far, -radius, radius, radius, -radius);
        return radius;
    }

    @Override
    public ArgumentSocket<DirectionalLight> getLight() {
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
    public Socket<? extends Collection<ShadowMap>> getShadowMaps() {
        return shadowMaps;
    }

    public ArgumentSocket<Float> getMaxDistance() {
        return maxDistance;
    }

}
