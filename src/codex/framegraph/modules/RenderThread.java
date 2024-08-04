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
package codex.framegraph.modules;

import codex.framegraph.FGRenderContext;
import codex.framegraph.FrameGraph;
import codex.framegraph.IndexSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes queued modules on another thread.
 * 
 * @author codex
 */
public class RenderThread extends RenderContainer<RenderModule> implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(RenderThread.class.getName());
    private static final long TIMEOUT_MILLIS = 5000;
    
    private FGRenderContext context;
    private Thread thread;
    private boolean executeNext = false;
    private boolean runAsync = true;
    
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
            frameGraph.notifyThreadComplete(this);
            long start = System.currentTimeMillis();
            while (!executeNext && thread != null && !isInterrupted()) {
                // wait for next execution, or until interrupted or stopped
                if (System.currentTimeMillis()-start >= TIMEOUT_MILLIS) {
                    LOG.log(Level.WARNING, "Thread {0} timed out waiting for next execution.", index.threadIndex);
                    thread = null;
                }
            }
            executeNext = false;
            if (thread == null || isInterrupted()) {
                break;
            }
        }
    }
    @Override
    public void updateModuleIndex(IndexSupplier supplier) {
        if (runAsync) {
            supplier.getNextInQueue(index);
            supplier.getNextThread();
            for (RenderModule m : queue) {
                m.updateModuleIndex(supplier);
            }
            supplier.continueFromIndex(index);
        } else {
            super.updateModuleIndex(supplier);
        }
    }
    @Override
    public void prepareModuleRender(FGRenderContext context) {
        super.prepareModuleRender(context);
        if (!queue.isEmpty() && runAsync) {
            frameGraph.registerExecutingThread(this);
        } else {
            thread = null;
        }
    }
    @Override
    public void executeModuleRender(FGRenderContext context) {
        if (!runAsync) {
            super.executeModuleRender(context);
        }
    }
    @Override
    public void cleanupModule(FrameGraph frameGraph) {
        thread = null;
    }
    
    /**
     * Starts executing queued modules on a new thread.
     * 
     * @param context 
     */
    public void startThreadExecution(FGRenderContext context) {
        if (!runAsync) {
            throw new IllegalStateException("Not set as asynchronous.");
        }
        this.context = context;
        if (isInterrupted()) {
            frameGraph.notifyThreadComplete(this);
            return;
        }
        if (thread == null) {
            thread = new Thread(this);
            thread.start();
        } else {
            executeNext = true;
        }
    }
    
    /**
     * If true, child modules will be executed on a new thread.
     * <p>
     * Otherwise children will be executed as normal.
     * 
     * @param runAsync 
     */
    public void setRunAsync(boolean runAsync) {
        this.runAsync = runAsync;
        if (!this.runAsync) {
            thread = null;
            executeNext = false;
        }
    }
    
    /**
     * 
     * @return 
     */
    public boolean isRunAsync() {
        return runAsync;
    }
    
}
