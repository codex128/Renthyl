/*
 * Copyright (c) 2024, codex
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package codex.renthyl;

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
        queue++;
        return store;
    }
    
    /**
     * Gets the current thread.
     * 
     * @return 
     */
    public int getCurrentThread() {
        return thread;
    }
    
    /**
     * Sets the current thread index.
     * 
     * @param thread 
     */
    public void setCurrentThread(int thread) {
        this.thread = thread;
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
