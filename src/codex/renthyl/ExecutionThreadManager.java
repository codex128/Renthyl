/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

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
            ThreadQueue t = new ThreadQueue();
            threads.add(t);
            aliveThreads++;
            return t;
        }
        ThreadQueue t = threads.get(i);
        if (t == null) {
            t = new ThreadQueue();
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
     * @param runnable
     * @param index 
     */
    public void add(Runnable runnable, int index) {
        getThread(index).add(runnable);
    }
    
    /**
     * Starts all queues.
     */
    public void start() {
        for (ThreadQueue t : threads) {
            if (t.start()) {
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
            t.terminate(force);
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
        
        private final LinkedList<Runnable> queue = new LinkedList<>();
        private Thread thread;
        private boolean run = false;
        private boolean used = false;
        
        @Override
        public void run() {
            while (true) {
                for (Runnable r : queue) {
                    r.run();
                    if (thread == null) {
                        break;
                    }
                }
                queue.clear();
                run = false;
                activeThreads--;
                long start = System.currentTimeMillis();
                while (!run && thread != null) {
                    if (System.currentTimeMillis()-start >= timeout) {
                        LOG.log(Level.WARNING, "Execution thread timed out after {0} milliseconds.", timeout);
                        thread = null;
                    }
                }
                if (thread == null) {
                    break;
                }
            }
        }
        
        /**
         * Adds the Runnable to the execution queue.
         * 
         * @param runnable 
         */
        public void add(Runnable runnable) {
            if (run) {
                throw new ConcurrentModificationException("Cannot add to queue while executing.");
            }
            queue.add(runnable);
        }
        
        /**
         * Starts executing the queue if the queue is not empty.
         * 
         * @return 
         */
        public boolean start() {
            if (!queue.isEmpty()) {
                run = true;
                used = true;
                if (thread == null) {
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
            if (force && run) {
                thread.interrupt();
            }
            thread = null;
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
        
    }
    
}
