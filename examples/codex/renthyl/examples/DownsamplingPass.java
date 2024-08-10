/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.examples;

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.definitions.TextureDef;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class DownsamplingPass extends RenderPass {

    private ResourceTicket<Texture2D> in;
    private ResourceTicket<Texture2D> out;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        
        // Create the input ticket, which will allow this pass to receive
        // a texture from another pass.
        in = addInput("Input");
        
        // Create the output ticket, which will allow this pass to
        // give the result texture to another pass for further processing.
        out = addOutput("Output");
        
        // Setup the output texture's min and mag filters.
        texDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texDef.setMagFilter(Texture.MagFilter.Nearest);
        
    }
    
    @Override
    protected void prepare(FGRenderContext context) {
        
        // Declare the output texture using the texture definition, indicating
        // we want to create a texture during execution.
        declare(texDef, out);
        
        // Reserve the resource used for output last time. Greatly
        // increases the chances of getting the same texture from
        // frame to frame, which minimizes texture binds (expensive).
        reserve(out);
        
        // Reference the input texture, indicating we want to use the
        // input texture during execution.
        reference(in);
        
    }
    
    @Override
    protected void execute(FGRenderContext context) {
        
        // Acquire the input texture. An exception is thrown if it cannot be found.
        Texture2D inTex = resources.acquire(in);
        Image img = inTex.getImage();
        
        // Set the output texture's demensions to half the input texture's demensions.
        int w = img.getWidth() / 2;
        int h = img.getHeight() / 2;
        texDef.setSize(w, h);
        
        // Set the ouptut texture's format to the input texture's format.
        texDef.setFormat(img.getFormat());
        
        // Get a FrameBuffer to render to that is the same size as the output texture.
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        
        // Acquire the output texture and attach it to the FrameBuffer as a color target.
        // This method is designed to minimize texture binds.
        resources.acquireColorTarget(fb, out);
        
        // Setup the FrameBuffer for rendering.
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        
        // Resize the camera to the output texture's size. Otherwise only a corner
        // of the input texture will be rendered to the output texture.
        context.resizeCamera(w, h, false, false, false);
        
        // Render the input texture to the output texture.
        context.renderTextures(inTex, null);
        
    }
    
    @Override
    protected void reset(FGRenderContext context) {
        // No reset needed.
    }
    
    @Override
    protected void cleanup(FrameGraph frameGraph) {
        // No cleanup needed.
    }
    
    @Override
    protected void write(OutputCapsule out) throws IOException {
        
        // Save the output texture's min and mag filters to the file.
        out.write(texDef.getMinFilter(), "minFilter", Texture.MinFilter.BilinearNoMipMaps);
        out.write(texDef.getMagFilter(), "magFilter", Texture.MagFilter.Bilinear);
        
    }
    
    @Override
    protected void read(InputCapsule in) throws IOException {
        
        // Read the output texture's min and mag filters from the file.
        texDef.setMinFilter(in.readEnum("minFilter", Texture.MinFilter.class, Texture.MinFilter.BilinearNoMipMaps));
        texDef.setMagFilter(in.readEnum("magFilter", Texture.MagFilter.class, Texture.MagFilter.Bilinear));
        
    }
    
}
