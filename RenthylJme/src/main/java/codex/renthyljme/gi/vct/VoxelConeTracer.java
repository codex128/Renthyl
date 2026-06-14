/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.gi.vct;

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

    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final TransitiveSocket<LightBuffer> lightBuffer = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> lightContribution = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> result = new TransitiveSocket<>(this);
    private final Attribute<Integer> gridSize = new Attribute<>(64);
    private final Attribute<BoundingBox> voxelBounds = new Attribute<>(new BoundingBox(new Vector3f(0, 0, 0), 20, 20, 20));
    
    public VoxelConeTracer(AssetManager assetManager, ResourceAllocator allocator) {

        addSockets(shadowMaps, geometry, lightBuffer, lightContribution, result);

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

        result.setUpstream(indirect.getResult());

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

    public Attribute<Integer> getGridSize() {
        return gridSize;
    }

    public Attribute<BoundingBox> getVoxelBounds() {
        return voxelBounds;
    }

}
