package codex.renthyl.resources;

import codex.renthyl.definitions.ResourceDef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Allocates resources with the intention of recycling them to meet
 * future allocation requests.
 */
public class ShortTermAllocator implements ResourceAllocator<CacheableWrapper> {

    private final Map<Long, AllocatedResource> resources = new ConcurrentHashMap<>();
    private long nextId = 0;
    private int retryThreshold = 4;
    private int timeoutLength = 1;

    @Override
    public CacheableWrapper allocate(ResourceDef def, int start, int end) {
        AllocatedResource target = null;
        float lowestEval = Float.MAX_VALUE;
        int selections = 0;
        for (AllocatedResource res : resources.values()) if (res.isAvailable()) {
            Float eval = def.evaluateResource(res.get());
            if (eval == null) { // resource is completely unsuited
                continue;
            }
            selections++;
            if (ResourceDef.isPerfectEvaluation(eval)) {
                if (!res.acquire(start, end)) { // resource is already acquired
                    continue;
                }
                return res;
            }
            if (target == null || eval < lowestEval) {
                target = res;
            }
        }
        if (target != null && target.acquire(start, end)) {
            return target;
        }
        if (selections >= retryThreshold) {
            return allocate(def, start, end);
        }
        target = new AllocatedResource(nextId++, timeoutLength, def.createResource(), def);
        if (!target.acquire(start, end)) {
            throw new IllegalStateException("Expected new resource to be acquirable.");
        }
        resources.put(target.getId(), target);
        return target;
    }

    @Override
    public CacheableWrapper getWrapperOf(Object resource) {
        return resources.values().stream().filter(w -> w.get() == resource).findAny().orElse(null);
    }

    /**
     * Disposes all unused resources held by this allocator.
     */
    public void flush() {
        for (Iterator<AllocatedResource> it = resources.values().iterator(); it.hasNext();) {
            AllocatedResource res = it.next();
            if (res.isAvailable() && !res.cycle()) {
                res.dispose();
                it.remove();
            }
        }
    }

    /**
     * Disposes all held resources.
     */
    public void clear() {
        resources.values().forEach(AllocatedResource::dispose);
        resources.clear();
    }

    /**
     * Sets the number of resources that passed in order for another attempt to be made
     * if a resource gets stolen. Resources can only be stolen in multithreaded renderings,
     * and even then it is very rare.
     *
     * <p>default=4</p>
     *
     * @param retryThreshold
     */
    public void setRetryThreshold(int retryThreshold) {
        this.retryThreshold = retryThreshold;
    }

    /**
     * Sets the number of {@link #flush() flushes} resources last without being used
     * before being disposed.
     *
     * <p>default=1</p>
     *
     * @param timeoutLength
     */
    public void setTimeoutLength(int timeoutLength) {
        this.timeoutLength = timeoutLength;
    }

    /**
     * Gets the retry threshold.
     *
     * @return
     */
    public int getRetryThreshold() {
        return retryThreshold;
    }

    /**
     * Gets the timeout length.
     *
     * @return
     */
    public int getTimeoutLength() {
        return timeoutLength;
    }

    private static class AllocatedResource <T> implements CacheableWrapper<T> {

        private final long id;
        private final Disposer<T> disposer;
        private final AtomicBoolean acquired = new AtomicBoolean(false);
        private final int timeoutLength;
        private T resource;
        private int timeout;
        private boolean cached = false;

        public AllocatedResource(long id, int timeout, T resource, Disposer<T> disposer) {
            this.id = id;
            this.timeoutLength = this.timeout = timeout;
            this.resource = Objects.requireNonNull(resource, "Resource cannot be null.");
            this.disposer = Objects.requireNonNull(disposer, "Disposer cannot be null.");
        }

        @Override
        public boolean acquire(int start, int end) {
            return isAvailable() && !acquired.getAndSet(true);
        }

        @Override
        public T get() {
            return resource;
        }

        @Override
        public void release() {
            if (!acquired.getAndSet(false)) {
                throw new IllegalStateException("Resource was not acquired.");
            }
            timeout = timeoutLength;
        }

        @Override
        public boolean isAvailable() {
            return resource != null && !cached && !acquired.get();
        }

        @Override
        public void cache(boolean cache) {
            this.cached = cache;
        }

        public void dispose() {
            disposer.dispose(resource);
            resource = null;
        }

        public boolean cycle() {
            return resource != null && (cached || acquired.get() || timeout-- > 0);
        }

        public long getId() {
            return id;
        }

    }

}
