package codex.renthyljme.render;

import codex.renthyl.render.queue.BasicRenderingQueue;

import java.util.concurrent.Executor;

public class JmeRenderingQueue extends BasicRenderingQueue {

    public JmeRenderingQueue() {}

    public JmeRenderingQueue(Executor service) {
        super(service);
    }

}
