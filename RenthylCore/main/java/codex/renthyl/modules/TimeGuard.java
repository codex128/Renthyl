/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.modules;

import codex.renthyl.ExecutionQueueList;
import codex.renthyl.ExecutionThreadManager;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import com.jme3.renderer.RendererException;
import java.util.function.Consumer;

/**
 *
 * @author gary
 */
public class TimeGuard extends RenderModule {
    
    private int threadIndex;
    private long timeout;

    public TimeGuard(int threadIndex) {
        this(threadIndex, 5000);
    }
    public TimeGuard(int threadIndex, long timeout) {
        this.threadIndex = threadIndex;
        this.timeout = timeout;
    }
    
    @Override
    protected void initModule(FrameGraph frameGraph) {}
    @Override
    public void queueModule(FGRenderContext context, ExecutionQueueList queues, int parentThread) {
        if (threadIndex < 0) threadIndex = parentThread;
        index.set(queues.add(this, threadIndex));
    }
    @Override
    protected void prepareModuleRender(FGRenderContext context) {}
    @Override
    protected void executeRender(FGRenderContext context) {
        ExecutionThreadManager threadManager = context.getPipelineContext().getThreadManager();
        long start = System.currentTimeMillis();
        while (threadManager.getNumActiveThreads() > 1) {
            if (System.currentTimeMillis()-start >= timeout) {
                throw new RendererException("Rendering time exceeded "+timeout+" milliseconds.");
            }
        }
    }
    @Override
    protected void resetRender(FGRenderContext context) {}
    @Override
    protected void cleanupModule(FrameGraph frameGraph) {}
    @Override
    public void renderingComplete() {}
    @Override
    public void traverse(Consumer<RenderModule> traverser) {}
    
}
