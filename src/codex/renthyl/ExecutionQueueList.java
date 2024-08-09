/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl;

import codex.renthyl.modules.RenderModule;
import java.util.ArrayList;
import java.util.LinkedList;

/**
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
