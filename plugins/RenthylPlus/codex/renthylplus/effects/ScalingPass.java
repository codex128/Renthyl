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
package codex.renthylplus.effects;

import codex.renthyl.modules.RenderPass;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.definitions.TextureDef;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class ScalingPass extends RenderPass {

    private static final Logger logger = Logger.getLogger(ScalingPass.class.getName());
    
    private ResourceTicket<Texture2D> inTex, outTex, tweenTex;
    private TextureDef<Texture2D> outTexDef, tweenTexDef;
    private Material material;
    private int renders = 5;
    private boolean downsample = true;
    private final Vector2f tempTexelSize = new Vector2f();
    
    public ScalingPass() {
        autoTicketRelease = false;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inTex = addInput("Texture");
        outTex = addOutput("Texture");
        tweenTex = new ResourceTicket<>();
        outTexDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
        tweenTexDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(outTexDef, outTex);
        reserve(outTex);
        reference(inTex);
    }
    @Override
    protected void execute(FGRenderContext context) {
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        Texture2D prev = resources.acquire(inTex);
        int w = prev.getImage().getWidth();
        int h = prev.getImage().getHeight();
        if (downsample) {
            capNumRenders(w, h);
        }
        for (int i = 0; i < renders-1; i++) {
            w = increment(w);
            h = increment(h);
            declare(tweenTexDef, tweenTex);
            prev = render(context, tweenTex, tweenTexDef, prev, w, h);
            resources.release(tweenTex);
        }
        w = increment(w);
        h = increment(h);
        render(context, outTex, outTexDef, prev, w, h);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    private int increment(int n) {
        if (downsample) return n >> 1;
        else return n << 1;
    }
    private void capNumRenders(int w, int h) {
        int limit = Math.min(w, h);
        for (int i = 0; i < renders; i++) {
            limit = limit >> 1;
            if (limit <= 2) {
                renders = i;
                logger.log(Level.INFO, "Number of renders capped at {0} due to texture size.", i);
                break;
            }
        }
    }
    private Texture2D render(FGRenderContext context, ResourceTicket<Texture2D> ticket,
            TextureDef def, Texture2D prev, int w, int h) {
        def.setSize(w, h);
        def.setFormat(prev.getImage().getFormat());
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        Texture2D tex = resources.acquireColorTarget(fb, ticket);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        if (material != null) {
            renderMaterial(context, prev, w, h);
        } else {
            context.renderTextures(prev, null);
        }
        return tex;
    }
    
    protected void renderMaterial(FGRenderContext context, Texture2D texture, int w, int h) {
        material.setTexture("Texture", texture);
        material.setVector2("TexelSize", tempTexelSize.set(1f/w, 1f/h));
        context.getScreen().render(context.getRenderManager(), material);
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
    public void setDownsample(boolean downsample) {
        this.downsample = downsample;
    }
    
    public Material getMaterial() {
        return material;
    }
    public boolean isDownsample() {
        return downsample;
    }
    
}
