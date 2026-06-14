package codex.renthyl.tasks.utils;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyl.tasks.AbstractTask;

public class AllocationTask <D extends ResourceDef<T>, T> extends AbstractTask {

    private final DefinedAllocationSocket<D, T> socket;

    public AllocationTask(ResourceAllocator allocator, D def) {
        socket = addSocket(new DefinedAllocationSocket<>(this, allocator, def));
    }

    @Override
    protected void renderTask() {
        socket.acquire();
    }

    public Socket<T> get() {
        return socket;
    }

    public D getDef() {
        return socket.getDef();
    }

}
