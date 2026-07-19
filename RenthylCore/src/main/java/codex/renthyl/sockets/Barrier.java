package codex.renthyl.sockets;

import codex.renthyl.render.Renderable;
import codex.renthyl.tasks.AbstractTask;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Blocks downstream tasks from running until a set of specific tasks completes.
 *
 * @param <T>
 */
public class Barrier <T> extends AbstractTask implements PointerSocket<T> {

    private final Collection<Renderable> await = new ArrayList<>();
    private Socket<? extends T> upstream;
    private int activeRefs = 0;

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return (upstream == null || upstream.isAvailableToDownstream(queuePosition)) && await.stream().allMatch(Renderable::isRenderingComplete);
    }

    @Override
    public void setUpstream(Socket<? extends T> upstream) {
        this.upstream = upstream;
    }

    @Override
    public Socket<? extends T> getUpstream() {
        return upstream;
    }

    @Override
    public T acquire() {
        return upstream != null ? upstream.acquire() : null;
    }

    @Override
    public void resetSocket() {}

    @Override
    public int getResourceUsage() {
        return upstream == null ? activeRefs : Math.max(activeRefs, upstream.getResourceUsage());
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (upstream != null) {
            upstream.reference(queuePosition);
        }
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("More releases than references.");
        }
        if (upstream != null) {
            upstream.release(queuePosition);
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    protected void renderTask() {}

    public void add(Renderable r) {
        await.add(r);
    }

    public boolean remove(Renderable r) {
        return await.remove(r);
    }

}
