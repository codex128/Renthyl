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

import codex.renthyl.modules.ModuleIndex;
import codex.renthyl.modules.RenderModule;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Contains an array of queues, one queue per thread, which
 * modules are added to for execution.
 * 
 * @author codex
 */
public class ExecutionQueueList {
    
    private final ArrayList<LinkedList<RenderModule>> queues = new ArrayList<>();
    private final ModuleIndex tempIndex = new ModuleIndex();
    private int activeQueues = 0;
    
    public ExecutionQueueList() {}
    
    /**
     * Adds the module to the queue at the index.
     * <p>
     * If the index is greater than the number of queues,
     * queues will be added.
     * 
     * @param module
     * @param index
     * @return assigned queue index (do not use resulting object)
     */
    public ModuleIndex add(RenderModule module, int index) {
        while (index >= queues.size()) {
            queues.add(null);
        }
        LinkedList<RenderModule> queue = queues.get(index);
        if (queue == null) {
            queue = new LinkedList<>();
            queues.set(index, queue);
            activeQueues++;
        }
        tempIndex.set(index, queue.size());
        queue.add(module);
        return tempIndex;
    }
    
    /**
     * Clears all queues.
     */
    public void flush() {
        for (int i = queues.size()-1; i >= 0; i--) {
            LinkedList<RenderModule> queue = queues.get(i);
            if (queue != null) {
                if (queue.isEmpty()) {
                    queues.set(i, null);
                    activeQueues--;
                } else {
                    queue.clear();
                }
            }
        }
    }
    
    /**
     * Gets the queue at the index.
     * 
     * @param i
     * @return queue (may be null)
     */
    public LinkedList<RenderModule> getQueue(int i) {
        return queues.get(i);
    }
    
    /**
     * Gets the number of queues.
     * <p>
     * Null queues are counted.
     * 
     * @return 
     */
    public int size() {
        return queues.size();
    }
    
    /**
     * Gets the number of queues that are in use.
     * 
     * @return 
     */
    public int getNumActiveQueues() {
        return activeQueues;
    }
    
}
