/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph;

/**
 *
 * @author codex
 */
public class IndexSupplier {
    
    private int thread = 0;
    private int queue = 0;
    private int nextThread = 1;
    
    /**
     * Gets the next index in the queue by incrementing
     * the queue index.
     * 
     * @return 
     */
    public ModuleIndex getNextInQueue() {
        return getNextInQueue(null);
    }
    
    /**
     * Gets the next index in the queue by incrementing the queue
     * index and storing the result in the given index.
     * 
     * @param store index to store result in, or null to create a new index
     * @return 
     */
    public ModuleIndex getNextInQueue(ModuleIndex store) {
        if (store == null) {
            store = new ModuleIndex();
        }
        store.threadIndex = thread;
        store.queueIndex = queue++;
        return store;
    }
    
    /**
     * Gets the next thread by incrementing the thread index and
     * resetting the queue index.
     * 
     * @return 
     */
    public ModuleIndex getNextThread() {
        return getNextThread(null);
    }
    
    /**
     * Gets the next thread by incrementing the thread index and
     * resetting the queue index, then storing the result in the
     * given index.
     * 
     * @param store
     * @return 
     */
    public ModuleIndex getNextThread(ModuleIndex store) {
        if (store == null) {
            store = new ModuleIndex();
        }
        store.threadIndex = thread = nextThread++;
        store.queueIndex = queue = 0;
        return store;
    }
    
    /**
     * Sets the next thread and queue indices to proceed the
     * given index.
     * 
     * @param index 
     */
    public void continueFromIndex(ModuleIndex index) {
        thread = index.threadIndex;
        queue = index.queueIndex+1;
    }
    
    /**
     * Resets all indices.
     */
    public void reset() {
        thread = queue = 0;
        nextThread = 1;
    }
    
    /**
     * Gets the number of active threads, including the main thread.
     * 
     * @return 
     */
    public int getNumActiveThreads() {
        return thread+1;
    }
    
}
