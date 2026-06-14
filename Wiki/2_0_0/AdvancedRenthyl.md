# Advanced Renthyl

### Frame

Oftentimes, it is handy to have a task manage a number of "child" tasks. Child tasks on their own are absolutely nothing special. They are just like any other task, they just happen to be managed by another task. The problems with child tasks arise if a chain of child tasks depends on a socket on the parent task, and the output from that chain passes through another socket on the parent task. This is a *looped dependency*, where a set of connection forms a circle, that would technically make the tasks in the circle run infinitely. Fortunately in such cases, none of tasks in the circle have all their dependencies satisfied, so nothing can run (resulting in a timeout exception).

Frame solves this problem by being a "psuedo-stageable" task. It can be staged normally, but it is flagged so that the RenderingQueue will skip over it in the render step. More importantly, it is always flagged as having completed the render step, thus allowing its child tasks to run without interference (it is never actually rendered though).

```java
public class MyCustomFrame extends Frame {
    
    // nothing specific to implement
    
}
```

Frame does not require implementing any methods.

### Multithreading

RenderingQueues can execute queued tasks in parallel over any number of worker threads. The graph structure implicitly handles the necessary synchronization to ensure that previous tasks are properly complete before later tasks run.

The BasicRenderingQueue provides multithreading support when an Executor is provided to manage the worker threads and the number of workers is set to more than one.

```java
Executor service = Executors.newCachedThreadPool();
BasicRenderingQueue queue = new BasicRenderingQueue(service);
queue.setWorkers(3);
```

Then use `queue` in a FrameGraph.

```java
FrameGraph fg = new FrameGraph(queue);
```

### Custom RenderingQueue

Implementing single-threaded RenderingQueues is fairly straightforward.

```java
import java.util.LinkedList;

public class MyCustomQueue implements RenderingQueue {

    private final Queue<Renderable> queue = new LinkedList<>();

    @Override
    public int stage(Renderable task) {}

    @Override
    public void update(float tpf) {}

    @Override
    public void prepare() {}

    @Override
    public void render() {}

    @Override
    public void reset() {}

    @Override
    public Iterator<Renderable> iterator() {
        return queue.iterator();
    }

}
```

`stage` simply appends the given task to the *end* of the queue, and returns the index of that task in the queue.

```java
@Override
public int stage(Renderable task) {
    queue.add(task);
    return queue.size() - 1;
}
```

`update`, `prepare`, and `reset` propogate those rendering steps to the queued Renderables. `reset` is also responsible for clearing all tasks from the queue.

```java
@Override
public void update(float tpf) {
    for (Renderable r : queue) {
        r.update(tpf);
    }
}

@Override
public void prepare() {
    for (Renderable r : queue) {
        r.prepare();
    }
}

@Override
public void reset() {
    for (Renderable r : queue) {
        r.reset();
    }
    queue.clear();
}
```

`render` also propogates the render step, but only to the tasks that that return `false` for `skipRender`. Those that return `true` are just skipped for this step. We must also ensure that the task is ready to render.

```java
@Override
public void render() {
    for (Renderable r : queue) {
        if (!r.skipRender()) {
            if (!task.ready()) {
                throw new IllegalStateException("Failed to render next task: " + r);
            }
            r.render();
        }
    }
}
```

For multithreaded queues, instead of throwing an exception if a task is not ready, we would try to render the next task in the queue, and circle back to the previous task later. In single-threaded queues, the current task -- as long as you're iterating up the queue -- should *always* be ready, otherwise a ligitimate error is present (such as looped dependencies).

Note that it is required that `ready` be called right before a `render` call, since tasks are allowed to update states in that method. Usually, tasks will flip a "claimed" boolean, which will make it only return true for at most one `ready` call during a single frame.

### Custom Sockets


