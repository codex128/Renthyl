package codex.renthyljme;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.tasks.AbstractTask;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;

public abstract class RasterTask extends AbstractTask {

    protected final TransitiveSocket<FrameGraphContext> contextSocket = new TransitiveSocket<>(this);
    protected FrameGraphContext context; // easy handle for subclasses to access the context during render

    public RasterTask() {
        addSocket(contextSocket);
    }

    @Override
    public void preStage(GlobalAttributes globals) {
        if (!prestaged && contextSocket.getUpstream() == null) {
            contextSocket.setUpstream(globals.getAttribute(FrameGraphContext.CONTEXT_GLOBAL));
        }
        super.preStage(globals);
    }

    @Override
    public void render() {
        context = contextSocket.acquireOrThrow(getClass().getName() + " requires access to context.");
        AppProfiler profiler = context.getRenderManager().getProfiler();
        if (profiler != null) {
            profiler.appSubStep(getClass().getSimpleName());
        }
        super.render();
        if (profiler != null) {
            profiler.appSubStep(getClass().getSimpleName());
        }
        context = null; // set back to null so that context is used at the expected time only
    }

}
