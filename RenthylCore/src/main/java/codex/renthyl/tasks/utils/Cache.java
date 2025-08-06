package codex.renthyl.tasks.utils;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.resources.CacheableWrapper;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.macros.Macro;
import codex.renthyl.tasks.AbstractTask;
import codex.renthyl.tasks.Frame;

import java.util.Objects;

public class Cache <T> extends Frame implements PointerSocket<T>, Macro<T> {

    private final ResourceAllocator<CacheableWrapper> allocator;
    private CacheableWrapper<T> cachedWrapper;
    private Socket<? extends T> upstream;
    private boolean stagedUpstream = false;
    private int activeRefs = 0;

    public Cache(ResourceAllocator<CacheableWrapper> allocator) {
        this.allocator = allocator;
    }

    @Override
    public void setUpstream(Socket<? extends T> upstream) {
        assertUnqueued();
        this.upstream = upstream;
    }

    @Override
    public Socket<? extends T> getUpstream() {
        return upstream;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (position < AbstractTask.QUEUING) {
            position = AbstractTask.QUEUING;
            preStage(globals);
            if (cachedWrapper == null && upstream != null) {
                upstream.stage(globals, queue);
                stagedUpstream = true;
            }
            position = queue.stage(this);
        }
    }

    @Override
    public boolean isAvailableToDownstream(int queuePosition) {
        return cachedWrapper != null || isUpstreamAvailable(queuePosition);
    }

    @Override
    public boolean isUpstreamAvailable(int queuePosition) {
        return upstream == null || upstream.isAvailableToDownstream(queuePosition);
    }

    @Override
    public T acquire() {
        if (stagedUpstream) {
            T value = upstream.acquire();
            if (value != null) {
                cachedWrapper = allocator.getWrapperOf(value);
                Objects.requireNonNull(cachedWrapper, "Failed to locate wrapper for value.").cache(true);
            } else refresh();
        }
        return preview();
    }

    @Override
    public void resetSocket() {
        if (activeRefs != 0) {
            throw new IllegalStateException("Missing " + activeRefs + " release calls.");
        }
        stagedUpstream = false;
    }

    @Override
    public int getResourceUsage() {
        return stagedUpstream && upstream != null ? Math.max(activeRefs, upstream.getResourceUsage()) : activeRefs;
    }

    @Override
    public void reference(int queuePosition) {
        activeRefs++;
        if (stagedUpstream) {
            upstream.reference(queuePosition);
        }
    }

    @Override
    public void release(int queuePosition) {
        if (--activeRefs < 0) {
            throw new IllegalStateException("Unexpected release.");
        }
        if (stagedUpstream) {
            upstream.release(queuePosition);
        }
    }

    @Override
    public int getActiveReferences() {
        return activeRefs;
    }

    @Override
    public T preview() {
        return cachedWrapper != null ? cachedWrapper.get() : null;
    }

    public void refresh() {
        if (cachedWrapper != null) {
            cachedWrapper.cache(false);
            cachedWrapper = null;
        }
    }

}
