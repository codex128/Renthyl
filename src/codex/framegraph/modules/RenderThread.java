/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.modules;

import codex.framegraph.FGRenderContext;
import codex.framegraph.FrameGraph;
import codex.framegraph.IndexSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class RenderThread extends RenderContainer<RenderModule> implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(RenderThread.class.getName());
    private static final long TIMEOUT_MILLIS = 5000;
    
    private FGRenderContext context;
    private Thread thread;
    private boolean executeNext = false;
    
    @Override
    public void run() {
        while (true) {
            try {
                for (RenderModule m : queue) {
                    if (isInterrupted()) {
                        break;
                    }
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
        super.updateModuleIndex(supplier);
        supplier.getNextThread();
        for (RenderModule m : queue) {
            m.updateModuleIndex(supplier);
        }
        supplier.continueFromIndex(index);
    }
    @Override
    public void prepareModuleRender(FGRenderContext context) {
        super.prepareModuleRender(context);
        if (!queue.isEmpty()) {
            frameGraph.registerExecutingThread(this);
        }
    }
    @Override
    public void executeModuleRender(FGRenderContext context) {
        // do nothing
    }
    @Override
    public void cleanupModule(FrameGraph frameGraph) {
        thread = null;
    }
    
    public void startThreadExecution(FGRenderContext context) {
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
    
}
