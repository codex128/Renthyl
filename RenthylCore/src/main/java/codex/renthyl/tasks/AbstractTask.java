package codex.renthyl.tasks;

import codex.renthyl.GlobalAttributes;
import codex.renthyl.render.Renderable;
import codex.renthyl.render.queue.RenderingQueue;
import codex.renthyl.sockets.Socket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractTask implements Renderable {

    public static final int UNQUEUED = -2, QUEUING = -1, QUEUED = 0;

    protected final Collection<Socket> sockets = new ArrayList<>();
    private final AtomicBoolean claimed = new AtomicBoolean(false);
    private boolean renderComplete = false;
    protected boolean prestaged = false;
    protected int position = UNQUEUED;

    @Override
    public void preStage(GlobalAttributes globals) {
        prestaged = true;
    }

    @Override
    public void stage(GlobalAttributes globals, RenderingQueue queue) {
        if (position < QUEUING) {
            // call prestage in case it hasn't been called yet
            if (!prestaged) {
                preStage(globals);
            }
            // set flag in anticipation of callbacks from sockets
            position = QUEUING;
            // queue upstream before queueing this
            stageSockets(globals, queue);
            position = queue.stage(this);
        }
    }

    @Override
    public void update(float tpf) {
        sockets.forEach(s -> s.update(tpf));
    }

    @Override
    public void prepare() {
        if (position < QUEUED) {
            throw new IllegalStateException("Task is being prepared, but is not properly queued.");
        }
        sockets.forEach(s -> s.reference(position));
    }

    @Override
    public boolean ready() {
        return sockets.stream().allMatch(socket -> socket.isUpstreamAvailable(getPosition())) && !claimed.getAndSet(true);
    }

    @Override
    public void render() {
        renderTask();
        sockets.forEach(s -> s.release(position));
        renderComplete = true;
    }

    @Override
    public boolean isRenderingComplete() {
        return renderComplete;
    }

    @Override
    public void reset() {
        // reset flags
        claimed.set(false);
        prestaged = false;
        renderComplete = false;
        position = UNQUEUED;
        // reset sockets
        sockets.forEach(Socket::resetSocket);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    protected <T extends Socket> T addSocket(T socket) {
        if (socket == this) { // doing so would likely result in infinite recursion somewhere
            throw new IllegalArgumentException("Cannot add socket to itself.");
        }
        sockets.add(socket);
        return socket;
    }

    protected void stageSockets(GlobalAttributes globals, RenderingQueue queue) {
        for (Socket s : sockets) {
            s.stage(globals, queue);
        }
    }

    protected void assertUnqueued() {
        if (position > UNQUEUED) {
            throw new IllegalStateException("Must be unqueued for this operation.");
        }
    }

    protected void addSockets(Socket... sockets) {
        for (Socket s : sockets) {
            if (s == this) {
                throw new IllegalArgumentException("Cannot add socket to itself.");
            }
            this.sockets.add(s);
        }
    }

    protected void removeSocket(Socket socket) {
        sockets.remove(socket);
    }

    protected abstract void renderTask();

    public boolean isClaimed() {
        return claimed.get();
    }

    public int getPosition() {
        return position;
    }

}
