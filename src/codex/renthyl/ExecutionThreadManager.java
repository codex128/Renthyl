/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.modules.RenderModule;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
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
    
    private final ArrayList<ThreadQueue> threads;
    private FGRenderContext context;
    private int aliveThreads = 0;
    private int activeThreads = 0;
    
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
    
    private ThreadQueue getThread(int i) {
        if (i < 0) {
            throw new IndexOutOfBoundsException("Thread index cannot be less than zero.");
        }
        while (i > threads.size()) {
            threads.add(null);
            i--;
        }
        if (i == threads.size()) {
            ThreadQueue t = new ThreadQueue(i);
            threads.add(t);
            aliveThreads++;
            return t;
        }
        ThreadQueue t = threads.get(i);
        if (t == null) {
            t = new ThreadQueue(i);
            threads.set(i, t);
            aliveThreads++;
        }
        return t;
    }
    
    /**
     * Adds the Runnable to the thread at the index.
     * <p>
     * If the index is greater than the number of threads, a new thread
     * will be created.
     * 
     * @param module
     * @param index 
     * @return  
     */
    public ModuleIndex add(RenderModule module, int index) {
        return getThread(index).add(module);
    }
    
    /**
     * Starts all queues.
     * @param context
     */
    public void start(FGRenderContext context) {
        this.context = context;
        for (int i = threads.size()-1; i >= 0; i--) {
            ThreadQueue t = threads.get(i);
            if (t != null && t.start(i == 0)) {
                activeThreads++;
            }
        }
    }
    
    /**
     * Flushes all threads that have not been used.
     */
    public void flush() {
        for (int i = 0; i < threads.size(); i++) {
            ThreadQueue t = threads.get(i);
            if (t != null && !t.flush()) {
                threads.set(i, null);
                aliveThreads--;
            }
        }
    }
    
    /**
     * Terminates all threads.
     * @param force
     */
    public void terminateAll(boolean force) {
        for (ThreadQueue t : threads) {
            if (t != null) {
                t.terminate(force);
            }
        }
        threads.clear();
        aliveThreads = 0;
        activeThreads = 0;
    }
    
    /**
     * Gets the number of threads that are alive but not
     * necessarily running.
     * 
     * @return 
     */
    public int getNumAliveThreads() {
        return aliveThreads;
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
    
    private class ThreadQueue implements Runnable {
        
        private final ModuleIndex index;
        private final LinkedList<RenderModule> queue = new LinkedList<>();
        private Thread thread;
        private boolean run = false;
        private boolean interrupt = false;
        private boolean used = false;
        
        public ThreadQueue(int index) {
            this.index = new ModuleIndex(index, -1);
        }
        
        @Override
        public void run() {
            while (true) {
                for (RenderModule r : queue) {
                    r.executeModuleRender(context);
                    if (interrupt) {
                        break;
                    }
                }
                queue.clear();
                run = false;
                activeThreads--;
                long start = System.currentTimeMillis();
                while (!run && thread != null && !interrupt) {
                    if (System.currentTimeMillis()-start >= timeout) {
                        LOG.log(Level.WARNING, "Execution thread timed out after {0} milliseconds.", timeout);
                        thread = null;
                    }
                }
                if (thread == null || interrupt) {
                    interrupt = false;
                    thread = null;
                    break;
                }
            }
        }
        
        /**
         * Adds the Runnable to the execution queue.
         * 
         * @param module 
         */
        public ModuleIndex add(RenderModule module) {
            if (run) {
                throw new ConcurrentModificationException("Cannot add to queue while executing.");
            }
            index.setQueueIndex(queue.size());
            queue.add(module);
            return index;
        }
        
        /**
         * Starts executing the queue if the queue is not empty.
         * 
         * @param local run on the local thread
         * @return 
         */
        public boolean start(boolean local) {
            if (!queue.isEmpty()) {
                run = true;
                used = true;
                if (local) {
                    run();
                } else if (thread == null) {
                    thread = new Thread(this);
                    thread.start();
                }
                return true;
            }
            return false;
        }
        
        /**
         * Terminates the thread when execution has finished.
         * 
         * @param force forcibly interrupts the thread without waiting for the queue it finish
         */
        public void terminate(boolean force) {
            if (force && run && thread != null) {
                thread.interrupt();
            }
            thread = null;
            interrupt = true;
        }
        
        /**
         * Returns true if this thread has been used since
         * the last flush call.
         * 
         * @return 
         */
        public boolean flush() {
            if (used) return !(used = false);
            else return used;
        }
        
        /**
         * Returns true if the queue is empty.
         * 
         * @return 
         */
        public boolean isEmpty() {
            return queue.isEmpty();
        }
        
    }
    
}
