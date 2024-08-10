/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthylplus.deferred;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.GeometryQueue;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.material.MaterialAdapter;
import codex.renthyl.modules.RenderPass;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.util.function.Function;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;

/**
 * Renders information about a queue of geometries to a set of textures.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Geometry ({@link GeometryQueue}: queue of geometries to extract information from.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>GBufferData[5] ({@link Texture2D}): textures containing geometry information.</li>
 *   <li>NumRenders (int): number of geometries rendered, since not all geometries are guaranteed to be rendered.</li>
 * </ul>
 * Geometries that do not have a material with a "GBuffer" technique are not rendered.
 * 
 * @author codex
 */
public class GBufferPass extends RenderPass implements GeometryRenderHandler {
    
    private static final String GBUFFER_PASS = "GBufferPass";
    private static final MaterialAdapter adapter = new MaterialAdapter();
    
    static {
        adapter.add("Common/MatDefs/Light/PBRLighting.j3md", "RenthylPlus/MatDefs/GBuffer/PBRLighting.fgmt");
        adapter.add("Common/MatDefs/Light/Lighting.j3md", "RenthylPlus/MatDefs/GBuffer/Lighting.fgmt");
        adapter.add("Common/MatDefs/Misc/Unshaded.j3md", "RenthylPlus/MatDefs/GBuffer/Unshaded.fgmt");
        adapter.add("Common/MatDefs/Terrain/Terrain.j3md", "RenthylPlus/MatDefs/GBuffer/Terrain.fgmt");
        adapter.add("Common/MatDefs/Terrain/PBRTerrain.j3md", "RenthylPlus/MatDefs/GBuffer/PBRTerrain.fgmt");
        adapter.add("Common/MatDefs/Terrain/AdvancedPBRTerrain.j3md", "RenthylPlus/MatDefs/GBuffer/AdvancedPBRTerrain.fgmt");
        adapter.add("Common/MatDefs/Terrain/TerrainLighting.j3md", "RenthylPlus/MatDefs/GBuffer/TerrainLighting.fgmt");
    }
    
    private AssetManager assetManager;
    private ResourceTicket<GeometryQueue> geometry;
    private ResourceTicket<Texture2D>[] gbuffers;
    private ResourceTicket<Integer> numRendersTicket;
    private final TextureDef<Texture2D>[] texDefs = new TextureDef[5];
    private int numRenders = 0;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        geometry = addInput("Geometry");
        gbuffers = addOutputGroup("GBufferData", 5);
        numRendersTicket = addOutput("NumRenders");
        Function<Image, Texture2D> tex = img -> new Texture2D(img);
        texDefs[0] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[1] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[2] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[3] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA32F);
        texDefs[4] = new TextureDef<>(Texture2D.class, tex, Image.Format.Depth);
        this.assetManager = frameGraph.getAssetManager();
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth(), h = context.getHeight();
        for (int i = 0; i < gbuffers.length; i++) {
            texDefs[i].setSize(w, h);
            declare(texDefs[i], gbuffers[i]);
        }
        declare(null, numRendersTicket);
        reserve(gbuffers);
        reference(geometry);
        numRenders = 0;
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        fb.setMultiTarget(true);
        resources.acquireColorTargets(fb, gbuffers[0], gbuffers[1], gbuffers[2], gbuffers[3]);
        resources.acquireDepthTarget(fb, gbuffers[4]);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        GeometryQueue queue = resources.acquire(geometry);
        context.renderGeometry(queue, null, this);
        resources.setPrimitive(numRendersTicket, numRenders);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if (!adapter.adaptMaterial(assetManager, material, GBUFFER_PASS)) {
            return false;
        }
        material.selectTechnique(GBUFFER_PASS, rm);
        rm.renderGeometry(geom);
        numRenders++;
        return true;
    }
    
    public static void addMaterialAdaption(String matdef, String technique) {
        adapter.add(matdef, technique);
    }
    public static void adaptAllMaterials(AssetManager assetManager) {
        adapter.adaptAll(assetManager);
    }
    
}
