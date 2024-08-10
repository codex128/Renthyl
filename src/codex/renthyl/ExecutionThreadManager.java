/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.modules.RenderModule;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages threads for rendering.
 * 
 * @author codex
 */
public class ExecutionThreadManager {
    
    private static final Logger LOG = Logger.getLogger(ExecutionThreadManager.class.getName());
    private static long timeout = 5000;
    
    private final ArrayList<ThreadExecutor> threads;
    private FGRenderContext context;
    private int activeThreads = 0;
    private boolean error = false;
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final Lock threadLock = new ReentrantLock();
    
    /**
     * 
     */
    public ExecutionThreadManager() {
        this(5);
    }
    
    /**
     * 
     * @param threadsArrayLength 
     */
    public ExecutionThreadManager(int threadsArrayLength) {
        threads = new ArrayList<>(threadsArrayLength);
    }
    
    public void start(FGRenderContext context, ExecutionQueueList queues) {
        while (threads.size() < queues.size()) {
            threads.add(new ThreadExecutor(threads.size()));
        }
        locked.set(false);
        for (int i = queues.size(); i >= 0; i--) {
            List<RenderModule> q = queues.getQueue(i);
            if (q != null) {
                if (!locked.get() && i > 0) {
                    threadLock.lock();
                    locked.set(true);
                }
                threads.get(i).start(q);
            }
        }
    }
    
    /**
     * Flushes all threads that have not been used.
     */
    public void stop() {
        for (int i = 0; i < threads.size(); i++) {
            ThreadExecutor t = threads.get(i);
            if (t != null) {
                t.stop();
            }
        }
    }
    
    /**
     * Indicates that an error has occured and execution
     * should be interrupted.
     * 
     * @param force if true, all active threads (except the main thread) are interrupted
     */
    public void error(boolean force) {
        error = true;
        if (force) {
            for (int i = 1; i < threads.size(); i++) {
                threads.get(i).interrupt();
            }
            if (locked.getAndSet(false)) {
                threadLock.unlock();
            }
        }
        threads.clear();
    }
    
    /**
     * Gets the number of threads currently executing.
     * 
     * @return 
     */
    public int getNumActiveThreads() {
        return activeThreads;
    }
    
    /**
     * Returns true if an error occured during execution.
     * 
     * @return 
     */
    public boolean didErrorOccur() {
        return error;
    }
    
    /**
     * Gets the lock used for waiting until all threads are complete.
     * 
     * @return 
     */
    public Lock getThreadLock() {
        return threadLock;
    }
    
    /**
     * Returns true if the thread lock is currently acquired
     * by this ExecutionThreadManager.
     * 
     * @return 
     */
    public boolean isLocked() {
        return locked.get();
    }
    
    private void notifyThreadComplete() {
        if (--activeThreads <= 0 && locked.getAndSet(false)) {
            threadLock.unlock();
        }
    }
    
    private class ThreadExecutor implements Runnable {
        
        private final int index;
        private List<RenderModule> queue;
        private Thread thread;
        private boolean run = false;
        
        public ThreadExecutor(int index) {
            this.index = index;
        }
        
        @Override
        public void run() {
            while (true) {
                try {
                    for (RenderModule r : queue) {
                        if (error) {
                            break;
                        }
                        r.executeModuleRender(context);
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "An exception occured while executing thread "+index, ex);
                    error(false);
                }
                run = false;
                notifyThreadComplete();
                long start = System.currentTimeMillis();
                while (!run && !error && thread != null) {
                    if (System.currentTimeMillis()-start >= timeout) {
                        LOG.log(Level.WARNING, "Execution thread timed out after {0} milliseconds.", timeout);
                        thread = null;
                    }
                }
                if (thread == null || error) {
                    thread = null;
                    break;
                }
            }
        }
        
        public boolean start(List<RenderModule> queue) {
            assert queue != null : "Queue cannot be null";
            this.queue = queue;
            if (!queue.isEmpty()) {
                run = true;
                activeThreads++;
                if (index == 0) {
                    run();
                } else if (thread == null) {
                    thread = new Thread(this);
                    thread.start();
                }
                return true;
            }
            return false;
        }
        
        public void stop() {
            if (queue == null) {
                thread = null;
            }
            queue = null;
        }
        
        public void interrupt() {
            if (thread != null) {
                thread.interrupt();
            }
            thread = null;
        }
        
    }
    
    /**
     * Sets the number of milliseconds a thread will wait for the next
     * execution session before dying.
     * <p>
     * default=5000
     * 
     * @param timeout 
     */
    public static void setTimeoutMillis(long timeout) {
        ExecutionThreadManager.timeout = timeout;
    }
    
    /**
     * 
     * @return 
     */
    public static long getTimeoutMillis() {
        return timeout;
    }
    
}
