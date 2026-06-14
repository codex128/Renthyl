/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.lights;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.tasks.AbstractTask;
import codex.renthyljme.shadowsnew.ShadowMask;
import com.jme3.light.Light;

import java.util.Collection;
import java.util.List;

/**
 *
 * @author codex
 */
public class LightBufferPass extends AbstractTask {

    private final CollectorSocket<Light> lights = new CollectorSocket<>(this);
    private final TransitiveSocket<List<Light>> lightShadowMapping = new TransitiveSocket<>(this);
    private final AllocationSocket<LightBuffer> lightData;
    private final LightBufferDef dataDef = new LightBufferDef();

    public LightBufferPass(ResourceAllocator allocator) {
        addSockets(lights, lightShadowMapping);
        lightData = addSocket(new AllocationSocket<>(this, allocator, dataDef));
        dataDef.setPadding(3);
    }

    @Override
    protected void renderTask() {
        Collection<Light> l = lights.acquire();
        dataDef.setSize(l.size());
        lightData.acquire().fill(lights.acquire(), lightShadowMapping.acquire());
    }

    public void setDataBufferPadding(int padding) {
        dataDef.setPadding(padding);
    }

    public CollectorSocket<Light> getLights() {
        return lights;
    }

    public PointerSocket<List<Light>> getLightShadowMapping() {
        return lightShadowMapping;
    }

    public Socket<LightBuffer> getLightData() {
        return lightData;
    }

}
