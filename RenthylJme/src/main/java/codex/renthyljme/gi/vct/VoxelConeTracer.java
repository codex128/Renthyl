/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.gi.vct;

import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.macros.ArgumentMacro;
import codex.renthyl.tasks.utils.Multiplexor;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.tasks.Frame;
import codex.renthyl.tasks.attributes.Attribute;
import codex.renthyljme.lights.LightBuffer;
import codex.renthyljme.shadow.ShadowMap;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
@SuppressWarnings("FieldCanBeLocal")
public class VoxelConeTracer extends Frame {

    private final ArgumentSocket<Integer> gridSize = new ArgumentSocket<>(this, 64);
    private final ArgumentSocket<BoundingBox> voxelBounds = new ArgumentSocket<>(this, new BoundingBox(new Vector3f(0, 10.1f, 0), 20, 20, 20));
    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final TransitiveSocket<LightBuffer> lightBuffer = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> lightContribution = new TransitiveSocket<>(this);
    private final Multiplexor<Texture2D> result = new Multiplexor<>(0);
    
    public VoxelConeTracer(AssetManager assetManager, ResourceAllocator allocator) {

        addSockets(gridSize, shadowMaps, geometry, lightBuffer, lightContribution);

        Attribute<BoundingBox> voxelBounds = new Attribute<>(new BoundingBox(new Vector3f(0, 10.1f, 0), 20, 20, 20));
        VoxelShadowComposerPass voxShadows = new VoxelShadowComposerPass(assetManager, allocator);
        DirectLightingPass direct = new DirectLightingPass(allocator);
        VoxelizationPass voxels = new VoxelizationPass(assetManager, allocator);
        IndirectLightingPass indirect = new IndirectLightingPass(assetManager, allocator);

        voxShadows.getGridSize().setUpstream(gridSize);
        voxShadows.getVoxelBounds().setUpstream(voxelBounds);
        voxShadows.getShadowMaps().addCollectionSource(shadowMaps);

        direct.getGeometry().addCollectionSource(geometry);
        direct.getLights().setUpstream(lightBuffer);
        direct.getLightContribution().setUpstream(lightContribution);

        voxels.getGeometry().addCollectionSource(geometry);
        voxels.getGridSize().setUpstream(gridSize);
        voxels.getVoxelBounds().setUpstream(voxelBounds);
        voxels.getLights().setUpstream(lightBuffer);
        voxels.getLightContribution().setUpstream(voxShadows.getVoxelLight());

        indirect.getGBufferMap().setUpstream(direct.getGBufferMap());
        indirect.getGridSize().setUpstream(gridSize);
        indirect.getVoxelBounds().setUpstream(voxelBounds);
        indirect.getVoxels().setUpstream(voxels.getVoxels());

        result.addUpstream(indirect.getResult());
        result.addUpstream(direct.getGBufferMap().get("Color"));

    }

    public ArgumentSocket<Integer> getGridSize() {
        return gridSize;
    }

    public ArgumentSocket<BoundingBox> getVoxelBounds() {
        return voxelBounds;
    }

    public CollectorSocket<ShadowMap> getShadowMaps() {
        return shadowMaps;
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public PointerSocket<LightBuffer> getLightBuffer() {
        return lightBuffer;
    }

    public PointerSocket<Texture2D> getLightContribution() {
        return lightContribution;
    }

    public Socket<Texture2D> getResult() {
        return result;
    }

    public ArgumentMacro<Integer> getResultSelector() {
        return result.getIndex();
    }

}
