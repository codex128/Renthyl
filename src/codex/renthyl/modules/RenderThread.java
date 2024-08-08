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

import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.IndexSupplier;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes queued modules on another thread.
 * 
 * @author codex
 */
public class RenderThread extends RenderContainer<RenderModule> implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(RenderThread.class.getName());
    
    private FGRenderContext context;
    private int forcedThreadIndex = -1;
    
    public RenderThread() {}
    public RenderThread(int forcedThreadIndex) {
        this.forcedThreadIndex = forcedThreadIndex;
    }
    
    @Override
    public void run() {
        while (true) {
            try {
                for (RenderModule m : queue) {
                    if (isInterrupted()) break;
                    m.executeModuleRender(context);
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "An exception occured while executing RenderThread at index "+index.threadIndex+'.', ex);
                frameGraph.interruptRendering();
            }
        }
    }
    @Override
    public void updateModuleIndex(IndexSupplier supplier) {
        supplier.getNextInQueue(index);
        if (forcedThreadIndex >= 0) {
            supplier.setCurrentThread(forcedThreadIndex);
        } else {
            supplier.getNextThread();
        }
        index.threadIndex = supplier.getCurrentThread();
        for (RenderModule m : queue) {
            m.updateModuleIndex(supplier);
        }
        supplier.continueFromIndex(index);
    }
    @Override
    public void prepareModuleRender(FGRenderContext context) {
        super.prepareModuleRender(context);
        if (!queue.isEmpty() && isOrphaned()) {
            if (index.threadIndex > 0) {
                context.getPipelineContext().getThreadManager().add(this, index.threadIndex-1);
            } else {
                frameGraph.registerOrphanedMainThread(this);
            }
        }
    }
    @Override
    public void executeModuleRender(FGRenderContext context) {
        if (!isOrphaned()) {
            super.executeModuleRender(context);
        }
    }
    @Override
    public void cleanupModule(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        out.write(forcedThreadIndex, "forcedThreadIndex", -1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        forcedThreadIndex = in.readInt("forcedThreadIndex", -1);
    }

    /**
     * Sets the execution thread this will execute on.
     * <p>
     * If greater than one, will run on the specified thread.
     * If less than one, will run on the next unclaimed thread.
     * If zero, will run on the main thread.
     * <p>
     * default=-1
     * 
     * @param forcedThreadIndex 
     */
    public void setForcedThreadIndex(int forcedThreadIndex) {
        this.forcedThreadIndex = forcedThreadIndex;
    }
    
    /**
     * 
     * @return 
     */
    public int getForcedThreadIndex() {
        return forcedThreadIndex;
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
