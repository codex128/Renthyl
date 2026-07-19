/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.renthyl.sockets.Barrier;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.ValueSocket;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.Frame;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author codex
 */
public class ShadowManager extends Frame {

    private final AssetManager assetManager;
    private final ResourceAllocator allocator;
    private final DefinedAllocationSocket<ShadowMaskDef, ShadowMask> mask;
    private final ValueSocket<Integer> numLights = new ValueSocket<>(this, 0);
    private final ShadowQueuePass queues = new ShadowQueuePass();
    private final Barrier output = new Barrier();

    public ShadowManager(AssetManager assetManager, ResourceAllocator allocator) {
        this.assetManager = assetManager;
        this.allocator = allocator;
        addSockets(numLights);
        mask = addSocket(new DefinedAllocationSocket<>(this, allocator, new ShadowMaskDef(assetManager)));
    }

    public <T extends Occlusion> T addOcclusion(T occlusion) {
        occlusion.getOccluders().setUpstream(queues.getOccluders());
        occlusion.getReceivers().setUpstream(queues.getReceivers());
        occlusion.getShadowMask().setUpstream(mask);
        numLights.setValue(numLights.getValue() + 1);
        output.add(occlusion);
        return occlusion;
    }

    public boolean removeOcclusion(Occlusion occlusion) {
        return output.remove(occlusion);
    }

    public DirectionalShadowPass addDirectionalLightSource(Socket<DirectionalLight> socket, int size, int splits) {
        DirectionalShadowPass dsp = new DirectionalShadowPass(assetManager, allocator, size, splits);
        dsp.getLight().setUpstream(socket);
        addOcclusion(dsp);
        return dsp;
    }

    public PointShadowPass addPointLightSource(Socket<PointLight> socket, int size) {
        PointShadowPass psp = new PointShadowPass(assetManager, allocator, size);
        psp.getLight().setUpstream(socket);
        addOcclusion(psp);
        return psp;
    }

    public SpotShadowPass addSpotLightSource(Socket<SpotLight> socket, int size) {
        SpotShadowPass ssp = new SpotShadowPass(assetManager, allocator, size);
        ssp.getLight().setUpstream(socket);
        addOcclusion(ssp);
        return ssp;
    }

    public void removeLightSource(Socket socket) {
        for (Iterator<Occlusion> it = occluders.iterator(); it.hasNext();) {
            Occlusion p = it.next();
            if (p.getLight().getUpstream() == socket) {
                shadowMaps.removeCollectionSource(p.getShadowMaps());
                it.remove();
            }
        }
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return queues.getGeometry();
    }

    public Socket<GeometryQueue> getOccluders() {
        return queues.getOccluders();
    }

    public Socket<GeometryQueue> getReceivers() {
        return queues.getReceivers();
    }

    public Socket<List<ShadowMap>> getShadowMaps() {
        return shadowMaps;
    }

    public Socket<Integer> getNumLights() {
        return numLights;
    }

}
