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
package codex.renthyl.modules;

import codex.renthyl.ExecutionQueueList;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.client.GraphSource;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.SavableObject;
import java.io.IOException;

/**
 * Executes child modules on particular thread.
 * 
 * @author codex
 */
public class RenderThread extends RenderContainer<RenderModule> {
    
    private GraphSource<Integer> threadIndexSource;
    
    public RenderThread() {}
    public RenderThread(GraphSource<Integer> threadIndexSource) {
        this.threadIndexSource = threadIndexSource;
    }
    
    @Override
    public void queueModule(FGRenderContext context, ExecutionQueueList queues, int parentThread) {
        @SuppressWarnings("null")
        int i = threadIndexSource != null ?
                threadIndexSource.getGraphValue(frameGraph, context.getViewPort())
                : parentThread;
        if (i < 0) i = parentThread;
        index.set(queues.add(this, i));
        for (RenderModule m : queue) {
            m.queueModule(context, queues, i);
        }
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(new SavableObject(threadIndexSource, true), "threadIndexSource", SavableObject.NULL);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        threadIndexSource = in.readSavableObject("threadIndexSource", GraphSource.class);
    }

    /**
     * Sets the source for the thread index.
     * <p>
     * Negative indices will make this run on the same thread
     * as its parent.
     * 
     * @param threadIndexSource 
     */
    public void setThreadIndexSource(GraphSource<Integer> threadIndexSource) {
        this.threadIndexSource = threadIndexSource;
    }
    
    /**
     * 
     * @return 
     */
    public GraphSource<Integer> getThreadIndexSource() {
        return threadIndexSource;
    }
    
    /**
     * Returns true if this thread is not running on the
     * same thread as its parent, or its parent is null.
     * 
     * @return 
     */
    public boolean isOrphaned() {
        return parent == null || parent.index.threadIndex != index.threadIndex;
    }
    
}
