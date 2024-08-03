/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.framegraph.modules.effects;

import codex.framegraph.FGRenderContext;
import codex.framegraph.FrameGraph;
import codex.framegraph.ResourceTicket;
import codex.framegraph.definitions.TextureDef;
import codex.framegraph.modules.RenderPass;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * Port of {@link com.jme3.post.filters.CartoonEdgeFilter} to a RenderPass.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): scene color texture.</li>
 *   <li>Depth ({@link Texture2D}): scene depth texture.</li>
 *   <li>Normals ({@link Texture2D}): scene normals texture.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): resulting color texture.</li>
 * </ul>
 * 
 * @author codex
 */
public class CartoonEdgePass extends RenderPass {

    private ResourceTicket<Texture2D> color, depth, normals;
    private ResourceTicket<Texture2D> result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private float edgeWidth = 1.0f;
    private float edgeIntensity = 1.0f;
    private float normalThreshold = 0.5f;
    private float depthThreshold = 0.1f;
    private float normalSensitivity = 1.0f;
    private float depthSensitivity = 10.0f;
    private ColorRGBA edgeColor = new ColorRGBA(0, 0, 0, 1);
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        color = addInput("Color");
        depth = addInput("Depth");
        normals = addInput("Normals");
        result = addOutput("Color");
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/CartoonEdge.j3md");
        material.setFloat("EdgeWidth", edgeWidth);
        material.setFloat("EdgeIntensity", edgeIntensity);
        material.setFloat("NormalThreshold", normalThreshold);
        material.setFloat("DepthThreshold", depthThreshold);
        material.setFloat("NormalSensitivity", normalSensitivity);
        material.setFloat("DepthSensitivity", depthSensitivity);
        material.setColor("EdgeColor", edgeColor);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        reference(color, depth, normals);
        texDef.setSize(context.getWidth(), context.getHeight());
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTarget(fb, result);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        material.setTexture("Texture", resources.acquire(color));
        material.setTexture("DepthTexture", resources.acquire(depth));
        material.setTexture("NormalsTexture", resources.acquire(normals));
        context.renderFullscreen(material);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {
        material = null;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(edgeWidth, "edgeWidth", 1.0f);
        oc.write(edgeIntensity, "edgeIntensity", 1.0f);
        oc.write(normalThreshold, "normalThreshold", 0.5f);
        oc.write(depthThreshold, "depthThreshold", 0.1f);
        oc.write(normalSensitivity, "normalSensitivity", 1.0f);
        oc.write(depthSensitivity, "depthSensitivity", 10.0f);
        oc.write(edgeColor, "edgeColor", ColorRGBA.Black);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        edgeWidth = ic.readFloat("edgeWidth", 1.0f);
        edgeIntensity = ic.readFloat("edgeIntensity", 1.0f);
        normalThreshold = ic.readFloat("normalThreshold", 0.5f);
        depthThreshold = ic.readFloat("depthThreshold", 0.1f);
        normalSensitivity = ic.readFloat("normalSensitivity", 1.0f);
        depthSensitivity = ic.readFloat("depthSensitivity", 10.0f);
        edgeColor = (ColorRGBA)ic.readSavable("edgeColor", ColorRGBA.Black.clone());
    }
    
    /**
     * Return the depth sensitivity<br>
     * for more details see {@link #setDepthSensitivity(float depthSensitivity)}
     * @return the depth sensitivity
     */
    public float getDepthSensitivity() {
        return depthSensitivity;
    }

    /**
     * sets the depth sensitivity<br>
     * defines how much depth will influence edges, default is 10
     *
     * @param depthSensitivity the desired sensitivity (default=10)
     */
    public void setDepthSensitivity(float depthSensitivity) {
        this.depthSensitivity = depthSensitivity;
        if (material != null) {
            material.setFloat("DepthSensitivity", depthSensitivity);
        }
    }

    /**
     * returns the depth threshold<br>
     * for more details see {@link #setDepthThreshold(float depthThreshold)}
     * @return the threshold
     */
    public float getDepthThreshold() {
        return depthThreshold;
    }

    /**
     * sets the depth threshold<br>
     * Defines at what threshold of difference of depth an edge is outlined default is 0.1f
     *
     * @param depthThreshold the desired threshold (default=0.1)
     */
    public void setDepthThreshold(float depthThreshold) {
        this.depthThreshold = depthThreshold;
        if (material != null) {
            material.setFloat("DepthThreshold", depthThreshold);
        }
    }

    /**
     * returns the edge intensity<br>
     * for more details see {@link #setEdgeIntensity(float edgeIntensity) }
     * @return the intensity
     */
    public float getEdgeIntensity() {
        return edgeIntensity;
    }

    /**
     * sets the edge intensity<br>
     * Defines how visible the outlined edges will be
     *
     * @param edgeIntensity the desired intensity (default=1)
     */
    public void setEdgeIntensity(float edgeIntensity) {
        this.edgeIntensity = edgeIntensity;
        if (material != null) {
            material.setFloat("EdgeIntensity", edgeIntensity);
        }
    }

    /**
     * returns the width of the edges
     * @return the width
     */
    public float getEdgeWidth() {
        return edgeWidth;
    }

    /**
     * sets the width of the edge in pixels default is 1
     *
     * @param edgeWidth the desired width (in pixels, default=1)
     */
    public void setEdgeWidth(float edgeWidth) {
        this.edgeWidth = edgeWidth;
        if (material != null) {
            material.setFloat("EdgeWidth", edgeWidth);
        }
    }

    /**
     * returns the normals sensitivity<br>
     * form more details see {@link #setNormalSensitivity(float normalSensitivity)}
     * @return the sensitivity
     */
    public float getNormalSensitivity() {
        return normalSensitivity;
    }

    /**
     * Sets the normals sensitivity. Default is 1.
     *
     * @param normalSensitivity the desired sensitivity (default=1)
     */
    public void setNormalSensitivity(float normalSensitivity) {
        this.normalSensitivity = normalSensitivity;
        if (material != null) {
            material.setFloat("NormalSensitivity", normalSensitivity);
        }
    }

    /**
     * returns the normal threshold<br>
     * for more details see {@link #setNormalThreshold(float normalThreshold)}
     * 
     * @return the threshold
     */
    public float getNormalThreshold() {
        return normalThreshold;
    }

    /**
     * sets the normal threshold default is 0.5
     *
     * @param normalThreshold the desired threshold (default=0.5)
     */
    public void setNormalThreshold(float normalThreshold) {
        this.normalThreshold = normalThreshold;
        if (material != null) {
            material.setFloat("NormalThreshold", normalThreshold);
        }
    }

    /**
     * returns the edge color
     * @return the pre-existing instance
     */
    public ColorRGBA getEdgeColor() {
        return edgeColor;
    }

    /**
     * Sets the edge color, default is black
     *
     * @param edgeColor the desired color (alias created, default=(0,0,0,1))
     */
    public void setEdgeColor(ColorRGBA edgeColor) {
        this.edgeColor = edgeColor;
        if (material != null) {
            material.setColor("EdgeColor", edgeColor);
        }
    }
    
}