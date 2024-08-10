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
package codex.renthylplus;

import codex.renthyl.FrameGraph;
import codex.renthyl.modules.Attribute;
import codex.renthyl.modules.OutputPass;
import codex.renthyl.modules.Junction;
import codex.renthyl.modules.geometry.SceneEnqueuePass;
import codex.renthyl.modules.geometry.QueueMergePass;
import codex.renthyl.modules.geometry.OutputGeometryPass;
import codex.renthylplus.deferred.GBufferPass;
import codex.renthylplus.deferred.DeferredPass;
import codex.renthylplus.light.LightImagePass;
import codex.renthyl.client.GraphSetting;
import codex.renthyl.light.TiledRenderGrid;
import codex.renthyl.modules.RenderContainer;
import codex.renthyl.modules.RenderModule;
import codex.renthyl.modules.RenderThread;
import com.jme3.asset.AssetManager;

/**
 * Utility class for constructing common {@link FrameGraph}s in code.
 * 
 * @author codex
 */
public class FrameGraphFactory {
    
    private FrameGraphFactory() {}
    
    /**
     * Constructs a standard forward FrameGraph, with no controllable settings.
     * 
     * @param assetManager
     * @return forward framegraph
     */
    public static FrameGraph forward(AssetManager assetManager) {
        
        FrameGraph fg = new FrameGraph(assetManager);
        fg.setName("Forward");
        
        SceneEnqueuePass enqueue = fg.add(new SceneEnqueuePass(true, true));
        QueueMergePass merge = fg.add(new QueueMergePass(5));
        OutputGeometryPass out = fg.add(new OutputGeometryPass());
        
        merge.makeInput(enqueue, "Opaque", "Queues[0]");
        merge.makeInput(enqueue, "Sky", "Queues[1]");
        merge.makeInput(enqueue, "Transparent", "Queues[2]");
        merge.makeInput(enqueue, "Gui", "Queues[3]");
        merge.makeInput(enqueue, "Translucent", "Queues[4]");
        
        out.makeInput(merge, "Result", "Geometry");
        
        return fg;
        
    }
    
    /**
     * Constructs a deferred FrameGraph.
     * 
     * @param assetManager
     * @return deferred framegraph
     */
    public static FrameGraph deferred(AssetManager assetManager) {
        
        FrameGraph fg = new FrameGraph(assetManager);
        fg.setName("Deferred");
        
        GraphSetting<Integer> async = new GraphSetting<>("Async", 0);
        
        SceneEnqueuePass enqueue = fg.add(new SceneEnqueuePass(true, true));
        Attribute tileInfoAttr = fg.add(new Attribute());
        Junction tileJunct1 = fg.add(new Junction(1, 1));
        GBufferPass gbuf = fg.add(new GBufferPass());
        Junction gbufDebugTarget = fg.add(new Junction(5, 1));
        Attribute gbufDebug = fg.add(new Attribute());
        LightImagePass lightImg = fg.add(new RenderThread(async)).add(new LightImagePass());
        Junction lightJunct = fg.add(new Junction(1, 6));
        Junction tileJunct2 = fg.add(new Junction(1, 2));
        DeferredPass deferred = fg.add(new DeferredPass());
        OutputPass defOut = fg.add(new OutputPass(0f));
        QueueMergePass merge = fg.add(new RenderThread(async)).add(new QueueMergePass(4));
        OutputGeometryPass geometry = fg.add(new OutputGeometryPass());
        
        gbuf.makeInput(enqueue, "Opaque", "Geometry");
        
        gbufDebugTarget.setIndexSource(new GraphSetting("GBufferDebug", -1));
        gbufDebugTarget.makeGroupInput(gbuf, "GBufferData", Junction.getInput());
        
        gbufDebug.setName("GBufferDebug");
        gbufDebug.makeInput(gbufDebugTarget, Junction.getOutput(), Attribute.INPUT);
        
        GraphSetting<TiledRenderGrid> tileInfo = new GraphSetting<>("TileInfo", new TiledRenderGrid());
        tileInfoAttr.setSource(tileInfo);
        
        GraphSetting<Integer> tileToggle = new GraphSetting("UseLightTiling", -1);
        tileJunct1.makeInput(tileInfoAttr, Attribute.OUTPUT, Junction.getInput(0));
        tileJunct1.setIndexSource(tileToggle);
        
        lightImg.makeInput(enqueue, "OpaqueLights", "Lights");
        lightImg.makeInput(tileJunct1, Junction.getOutput(), "TileInfo");
        
        GraphSetting<Integer> lightPackMethod = new GraphSetting("UseLightTextures", -1);
        lightJunct.makeGroupInput(lightImg, "Textures", Junction.getInput(0), 0, 0, 3);
        lightJunct.makeInput(lightImg, "NumLights", Junction.getInput(0, 3));
        lightJunct.makeInput(lightImg, "Ambient", Junction.getInput(0, 4));
        lightJunct.makeInput(lightImg, "Probes", Junction.getInput(0, 5));
        lightJunct.setIndexSource(lightPackMethod);
        
        tileJunct2.makeGroupInput(lightImg, "TileTextures", Junction.getInput(0));
        tileJunct2.setIndexSource(tileToggle);
        
        deferred.makeGroupInput(gbuf, "GBufferData", "GBufferData");
        deferred.makeInput(enqueue, "OpaqueLights", "Lights");
        deferred.makeGroupInput(lightJunct, Junction.getOutput(), "LightTextures", 0, 0, 3);
        deferred.makeInput(lightJunct, Junction.getOutput(3), "NumLights");
        deferred.makeInput(lightJunct, Junction.getOutput(4), "Ambient");
        deferred.makeInput(lightJunct, Junction.getOutput(5), "Probes");
        deferred.makeGroupInput(tileJunct2, Junction.getOutput(), "TileTextures");
        
        defOut.makeInput(deferred, "Color", "Color");
        defOut.makeInput(gbuf, "GBufferData[4]", "Depth");
        
        merge.makeInput(enqueue, "Sky", "Queues[0]");
        merge.makeInput(enqueue, "Transparent", "Queues[1]");
        merge.makeInput(enqueue, "Gui", "Queues[2]");
        merge.makeInput(enqueue, "Translucent", "Queues[3]");
        
        geometry.makeInput(merge, "Result", "Geometry");
        
        return fg;
        
    }
    
    private static <T extends RenderModule> T add(T module, RenderContainer target1, RenderContainer target2, boolean val) {
        if (val) target1.add(module);
        else target2.add(module);
        return module;
    }
    
    /**
     * Utility class for creating test FrameGraphs.
     */
    public static class Test {
        
        private Test() {}
        
        public static FrameGraph testGBuffer(AssetManager assetManager) {
            
            FrameGraph fg = new FrameGraph(assetManager);
            
            SceneEnqueuePass enqueue = fg.add(new SceneEnqueuePass(true, true));
            QueueMergePass merge = fg.add(new QueueMergePass(5));
            GBufferPass gbuf = fg.add(new GBufferPass());
            Junction junct = fg.add(new Junction(5, 1));
            OutputPass out = fg.add(new OutputPass());
            
            merge.makeInput(enqueue, "Opaque", "Queues[0]");
            merge.makeInput(enqueue, "Sky", "Queues[1]");
            merge.makeInput(enqueue, "Transparent", "Queues[2]");
            merge.makeInput(enqueue, "Gui", "Queues[3]");
            merge.makeInput(enqueue, "Translucent", "Queues[4]");
            
            gbuf.makeInput(merge, "Result", "Geometry");
            
            junct.setIndexSource(new GraphSetting("GBuffer", 0));
            junct.makeGroupInput(gbuf, "GBufferData", Junction.getInput());
            
            out.makeInput(junct, Junction.getOutput(), "Color");
            
            return fg;
            
        }
        
    }
    
}
