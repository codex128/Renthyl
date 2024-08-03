/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.modules;

import codex.framegraph.FGRenderContext;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author codex
 */
public class RenderThread extends RenderContainer<RenderModule> implements Runnable {
    
    private static final Logger LOG = Logger.getLogger(RenderThread.class.getName());
    
    private FGRenderContext context;
    private Thread thread;
    private boolean executeNext = false;
    
    @Override
    public void run() {
        while (true) {
            try {
                executeModuleRender(context);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "An exception occured while executing RenderThread at index "+index.threadIndex+'.', ex);
                frameGraph.interruptRendering();
            } finally {
                frameGraph.notifyThreadComplete(this);
            }
            while (!executeNext && !isInterrupted() && thread != null) {}
            executeNext = false;
            thread = null;
            //if (isInterrupted() || exit) {
                break;
            //}
        }
    }
    
    /**
     * Starts running the group of modules contained by this RenderThread.
     * <p>
     * If the thread index of this module is asynchronous (index !=
     * {@link PassIndex#MAIN_THREAD}), a new thread is spawned for this
     * to execute on.
     * <p>
     * All exceptions that occur under this module are caught and 
     * the FrameGraph notified to ensure graceful interruption of other threads.
     * 
     * @param context 
     */
    public void startThreadExecution(FGRenderContext context) {
        this.context = context;
        if (isInterrupted()) {
            frameGraph.notifyThreadComplete(this);
            return;
        }
        if (!isAsync()) {
            run();
        } else if (thread == null) {
            thread = new Thread(this);
            thread.start();
        } else {
            executeNext = true;
        }
    }
    
    public void stopThreadExecution() {
        thread = null;
    }
    
}
