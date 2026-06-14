package codex.renthyl.tasks.utils;

import codex.renthyl.sockets.Socket;
import codex.renthyl.tasks.Frame;

import java.util.function.Function;

/**
 * Frame containing one socket.
 *
 * @param <T>
 */
public class SocketFrame <T extends Socket> extends Frame {

    private final T socket;

    public SocketFrame(Function<SocketFrame<T>, T> generator) {
        this.socket = addSocket(generator.apply(this));
    }

    /**
     * Returns the one socket.
     *
     * @return
     */
    public T get() {
        return socket;
    }

}
