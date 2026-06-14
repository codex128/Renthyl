package codex.renthyl.render.queue;

import codex.renthyl.render.Renderable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Basic rendering queue which can perform basic multithreading given
 * an {@link Executor} to handle the worker threads.
 */
public class BasicRenderingQueue implements RenderingQueue {

    private final Executor service;
    private final List<Worker> workers = new ArrayList<>();
    private final List<Renderable> staged = new ArrayList<>();
    private final Queue<Renderable> queue = new ConcurrentLinkedQueue<>();
    private final List<RenderingListener> listeners = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition inactive = lock.newCondition();
    private long inactiveTimeout = 5000;
    private Exception error;

    public BasicRenderingQueue() {
        this(null);
    }
    public BasicRenderingQueue(Executor service) {
        this.service = service;
        workers.add(new Worker());
    }

    @Override
    public int stage(Renderable task) {
        staged.add(task);
        return staged.size() - 1;
    }

    @Override
    public void update(float tpf) {
        for (Renderable r : staged) {
            r.update(tpf);
        }
    }

    @Override
    public void prepare() {
        forEach(Renderable::prepare);
    }

    @Override
    public void render() {
        // load from staged to queue
        for (Renderable r : staged) {
            if (!r.skipRender()) {
                queue.add(r);
            }
        }
        // execute renders
        Worker main = workers.get(0);
        if (service != null) for (Worker w : workers) {
            if (w != main) {
                service.execute(w);
            }
        }
        main.run();
        if (error != null) {
            throw new RuntimeException("Rendering failed with an exception: " + error.getMessage(), error);
        }
    }

    private void render(Worker worker) throws InterruptedException, TimeoutException {
        if (workers.size() == 1) {
            renderSingle(worker);
            return;
        }
        loop: while (!queue.isEmpty() && error == null) {
            int size = queue.size();
            for (Iterator<Renderable> it = queue.iterator(); it.hasNext(); ) {
                if (error != null) {
                    break;
                }
                Renderable r = it.next();
                // submit task for execution
                if (worker.submit(r)) {
                    it.remove();
                    worker.render(); // render submitted task
                    lock.lock();
                    inactive.signalAll(); // queue state has changed: signal waiting threads
                    lock.unlock();
                    continue loop; // search queue for next task
                }
            }
            // worker has become inactive
            // deadlocks are caught by the condition timeout
            lock.lock();
            while (size == queue.size() && error == null) {
                if (!inactive.await(inactiveTimeout, TimeUnit.MILLISECONDS)) {
                    throw new TimeoutException("Worker timed out waiting for tasks to complete. Queued to run: " + Arrays.toString(queue.toArray()));
                }
            }
            lock.unlock();
        }
    }

    private void renderSingle(Worker worker) throws TimeoutException {
        while (!queue.isEmpty()) {
            Renderable ex = queue.poll();
            if (worker.submit(ex)) {
                worker.render();
            } else {
                throw new TimeoutException("Failed to render next task: " + ex);
            }
        }
    }

    @Override
    public void reset() {
        staged.forEach(Renderable::reset);
        workers.forEach(Worker::clean);
        queue.clear();
        staged.clear();
        error = null;
    }

    @Override
    public Iterator<Renderable> iterator() {
        return staged.iterator();
    }

    /**
     * Registers the rendering event listener to receives callbacks for various
     * rendering events related to the RenderingQueue.
     *
     * @param listener
     */
    public void addListener(RenderingListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes the rendering event listener.
     *
     * @param listener
     */
    public void removeListener(RenderingListener listener) {
        listeners.remove(listener);
    }

    /**
     * Sets the number of worker threads assigned to rendering.
     *
     * <p>default=1</p>
     *
     * @param workers
     * @throws IllegalStateException if {@code workers} is less than one
     * @throws NullPointerException if {@code workers} is greater than one but no {@link Executor} is provided
     */
    public void setWorkers(int workers) {
        if (workers < 1) {
            throw new IllegalArgumentException("Must have at least one worker.");
        }
        if (workers > 1 && service == null) {
            throw new NullPointerException("No executor provided for multithreading.");
        }
        while (this.workers.size() < workers) {
            this.workers.add(new Worker());
        }
        while (this.workers.size() > workers) {
            this.workers.remove(this.workers.size() - 1);
        }
    }

    /**
     * Sets the number of milliseconds a worker thread may remain inactive before timing out.
     *
     * @param inactiveTimeout timeout duration in milliseconds (must be positive)
     */
    public void setInactiveTimeout(long inactiveTimeout) {
        assert inactiveTimeout > 0;
        this.inactiveTimeout = inactiveTimeout;
    }

    /**
     * Gets the number of worker threads assigned to rendering.
     *
     * @return
     */
    public int getWorkers() {
        return workers.size();
    }

    /**
     * Gets the nubmer of milliseconds a worker thread may remain inactive before timing out.
     *
     * @return
     */
    public long getInactiveTimeout() {
        return inactiveTimeout;
    }

    /**
     * Submits a rendering error and exits rendering.
     *
     * <p>The submitted error is thrown after rendering operations are safely terminated.
     * Further submitted errors are ignored.</p>
     *
     * @param error
     */
    protected void submitError(Exception error) {
        if (this.error != null) {
            return;
        }
        lock.lock();
        if (this.error == null) {
            this.error = error;
            inactive.signalAll();
        }
        lock.unlock();
    }

    /**
     * Runnable worker which renders tasks from the queue until none remain.
     */
    public class Worker implements Runnable {

        private Renderable task;

        @Override
        public void run() {
            try {
                BasicRenderingQueue.this.render(this);
            } catch (Exception e) {
                submitError(new RuntimeException("Worker render failed with an exception.", e));
            }
        }

        public boolean submit(Renderable task) {
            if (this.task != null || !task.ready()) {
                return false;
            }
            this.task = task;
            return true;
        }

        public void render() {
            if (task == null) {
                throw new NullPointerException("No renderable task submitted to execute.");
            }
            for (RenderingListener l : listeners) {
                l.onTaskBegin(task);
            }
            try {
                task.render();
            } catch (Exception ex) {
                submitError(ex);
            } finally {
                for (RenderingListener l : listeners) {
                    l.onTaskEnd(task);
                }
                task = null;
            }
        }

        public void clean() {
            task = null;
        }

    }

}
