/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules.cache;

import codex.boost.export.SavableObject;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import codex.renthyl.modules.RenderPass;
import codex.renthyl.resources.ResourceTicket;
import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import java.io.IOException;

/**
 *
 * @author codex
 */
public class CacheWrite extends RenderPass {
    
    public static final String INPUT = "Input";
    
    private GraphSource<String> keySource;
    private ResourceTicket input;
    
    public CacheWrite() {}
    public CacheWrite(GraphSource<String> keySource) {
        this.keySource = keySource;
    }
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        input = addInput(INPUT);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        referenceOptional(input);
    }
    @Override
    protected void execute(FGRenderContext context) {
        if (resources.acquireOrElse(input, null) != null) {
            String key = keySource.getGraphValue(frameGraph, context.getViewPort());
            if (key != null) {
                resources.cache(input, key);
            }
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean isUsed() {
        return true;
    }
    @Override
    protected void write(OutputCapsule out) throws IOException {
        out.write(new SavableObject(keySource), "keySource", SavableObject.NULL);
    }
    @Override
    protected void read(InputCapsule in) throws IOException {
        keySource = SavableObject.read(in, "keySource", GraphSource.class);
    }
    
    /**
     * Sets the source of the cache key to use.
     * 
     * @param keySource 
     */
    public void setKeySource(GraphSource<String> keySource) {
        this.keySource = keySource;
    }
    
    /**
     * Gets the source of the cache key to use.
     * 
     * @return 
     */
    public GraphSource<String> getKeySource() {
        return keySource;
    }
    
}
