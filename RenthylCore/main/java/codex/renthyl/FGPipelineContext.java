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
package codex.renthyl;

import codex.renthyl.resources.RenderObjectMap;
import codex.renthyl.debug.GraphEventCapture;
import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.renderer.pipeline.AbstractPipelineContext;
import com.jme3.renderer.RenderManager;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages global pipeline context for rendering with FrameGraphs.
 * 
 * @author codex
 */
public class FGPipelineContext extends AbstractPipelineContext {
    
    private static final Logger LOG = Logger.getLogger(FGPipelineContext.class.getName());
    
    private final RenderObjectMap renderObjects;
    private final ExecutionThreadManager threadManager = new ExecutionThreadManager();
    private GraphEventCapture eventCapture;
    
    public FGPipelineContext(RenderManager rm) {
        renderObjects = new RenderObjectMap(this, true);
    }

    @Override
    public void startRenderFrame(RenderManager rm) {
        if (eventCapture != null) {
            eventCapture.beginRenderFrame();
        }
        renderObjects.newFrame();
    }
    @Override
    public void endRenderFrame(RenderManager rm) {
        if (eventCapture != null) {
            eventCapture.endRenderFrame();
        }
        renderObjects.flushMap();
        threadManager.stop();
        if (eventCapture != null && eventCapture.isComplete()) {
            try {
                eventCapture.export();
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, "Error exporting captured event data.", ex);
            }
            eventCapture = null;
        }
    }

    public void setEventCapture(GraphEventCapture eventCapture) {
        this.eventCapture = eventCapture;
    }
    
    public void applicationStopped() {
        threadManager.stop();
    }
    
    public RenderObjectMap getRenderObjects() {
        return renderObjects;
    }
    public ExecutionThreadManager getThreadManager() {
        return threadManager;
    }
    public GraphEventCapture getEventCapture() {
        return eventCapture;
    }
    
}
