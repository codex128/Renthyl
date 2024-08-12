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

import com.jme3.renderer.DepthRange;
import com.jme3.renderer.Camera;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.GeometryComparator;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.NullComparator;
import com.jme3.scene.Geometry;
import com.jme3.util.ListSort;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.function.Function;

/**
 * Queue of ordered geometries for rendering.
 * <p>
 * Similar to {@link GeometryList}, but designed for use in FrameGraphs. Specifically,
 * this can store other GeometryQueues internally, essentially making queues able
 * to merge very quickly and still maintain geometry order.
 * 
 * @author codex
 */
public class GeometryQueue implements Iterable<Geometry> {
    
    private static final int DEFAULT_SIZE = 32;
    
    private Geometry[] geometries;
    private GeometryComparator comparator;
    private Camera cam;
    private final ListSort listSort;
    private final LinkedList<GeometryQueue> internalQueues = new LinkedList<>();
    private final DepthRange depth = new DepthRange();
    private boolean updateFlag = true;
    private boolean perspective = true;
    private int size = 0;
    
    /**
     * Geometry queue with default settings and a {@link NullComparator}.
     */
    public GeometryQueue() {
        this(new NullComparator());
    }
    /**
     * Geometry queue with default settings and the given comparator.
     * 
     * @param comparator 
     */
    public GeometryQueue(GeometryComparator comparator) {
        this(comparator, DEFAULT_SIZE);
    }
    /**
     * 
     * @param comparator
     * @param initialSize 
     */
    public GeometryQueue(GeometryComparator comparator, int initialSize) {
        this.comparator = comparator;
        geometries = new Geometry[initialSize];
        listSort = new ListSort<Geometry>();
    }
    
    /**
     * Sorts this queue and all internal queues.
     */
    public void sort() {
        if (updateFlag && size > 1) {
            // sort the spatial list using the comparator
            if (listSort.getLength() != size) {
                listSort.allocateStack(size);
            }
            listSort.sort(geometries, comparator);
            updateFlag = false;
        }
        for (GeometryQueue q : internalQueues) {
            q.sort();
        }
    }
    /**
     * Renders this queue and all internal queues.
     * 
     * @param renderManager 
     * @param handler 
     */
    public void render(RenderManager renderManager, GeometryRenderHandler handler) {
        if (handler == null) {
            handler = GeometryRenderHandler.DEFAULT;
        }
        renderManager.getRenderer().setDepthRange(depth);
        if (!perspective) {
            renderManager.setCamera(cam, true);
        }
        for (Geometry g : geometries) {
            if (g == null) continue;
            handler.renderGeometry(renderManager, g);
            g.queueDistance = Float.NEGATIVE_INFINITY;
        }
        if (!perspective) {
            renderManager.setCamera(cam, false);
        }
        renderManager.getRenderer().setDepthRange(DepthRange.IDENTITY);
        for (GeometryQueue q : internalQueues) {
            q.render(renderManager, handler);
        }
    }
    
    /**
     * Adds the geometry to the queue.
     * 
     * @param g 
     */
    public void add(Geometry g) {
        if (size == geometries.length) {
            Geometry[] temp = new Geometry[size * 2];
            System.arraycopy(geometries, 0, temp, 0, size);
            geometries = temp; // original list replaced by double-size list
        }
        geometries[size++] = g;
        updateFlag = true;
    }
    /**
     * Sets the element at the given index.
     *
     * @param index The index to set
     * @param value The value
     */
    public void set(int index, Geometry value) {
        geometries[index] = value;
        updateFlag = true;
    }
    /**
     * Adds the geometry queue.
     * 
     * @param q 
     */
    public void add(GeometryQueue q) {
        internalQueues.add(q);
    }
    /**
     * Adds the geometry queue at the index.
     * 
     * @param q
     * @param index 
     */
    public void add(GeometryQueue q, int index) {
        internalQueues.add(index, q);
    }

    /**
     * Resets list size to 0.
     * <p>
     * Clears internal queue list, but does not clear internal queues.
     */
    public void clear() {
        for (int i = 0; i < size; i++) {
            geometries[i] = null;
        }
        internalQueues.clear();
        updateFlag = true;
        size = 0;
    }
    
    /**
     * Removes all geometries from
     */
    public void removeAllGeometries() {
        
    }
    
    /**
     * Makes a copy of this queue's parameters (not geometries or internal queues).
     * 
     * @param includeInternalQueues
     * @return 
     */
    public GeometryQueue makeParamCopy(boolean includeInternalQueues) {
        GeometryQueue target = new GeometryQueue(comparator, size);
        target.cam = cam;
        target.depth.set(depth);
        target.updateFlag = updateFlag;
        target.perspective = perspective;
        if (includeInternalQueues) for (GeometryQueue q : internalQueues) {
            target.add(q.makeParamCopy(true));
        }
        return target;
    }
    
    /**
     * Makes a copy of this queue and all internal queues.
     * 
     * @return 
     */
    public GeometryQueue makeCopy() {
        GeometryQueue target = makeParamCopy(false);
        for (Geometry g : geometries) {
            target.add(g);
        }
        for (GeometryQueue q : internalQueues) {
            target.add(q.makeCopy());
        }
        return target;
    }
    
    /**
     * Generates a new GeometryQueue that contains only geometry
     * approved by the filter.
     * 
     * @param filter
     * @return 
     */
    public GeometryQueue cull(Function<Geometry, Boolean> filter) {
        GeometryQueue target = makeParamCopy(false);
        for (Geometry g : geometries) {
            if (!filter.apply(g)) {
                target.add(g);
            }
        }
        for (GeometryQueue q : internalQueues) {
            GeometryQueue result = q.cull(filter);
            if (result.containsGeometry()) {
                target.add(result);
            }
        }
        return target;
    }
    
    /**
     * Culls geometries from this queue and internal queues that are rejected
     * by the filter.
     * <p>
     * Internal queues that no longer contain geometry as a result of this
     * operation are removed.
     * 
     * @param filter
     * @return 
     */
    public GeometryQueue cullLocal(Function<Geometry, Boolean> filter) {
        int skip = 0;
        for (int i = 0; i < size; i++) {
            Geometry g = geometries[i-skip] = geometries[i];
            if (!filter.apply(g)) {
                geometries[i] = null;
                skip++;
            }
        }
        size -= skip;
        for (Iterator<GeometryQueue> it = internalQueues.iterator(); it.hasNext();) {
            if (it.next().cullLocal(filter).containsGeometry()) {
                it.remove();
            }
        }
        return this;
    }
    
    /**
     * Marks this list as requiring sorting.
     */
    public void setUpdateNeeded() {
        updateFlag = true;
    }
    /**
     * Sets the comparator used to sort geometries.
     * 
     * @param comparator 
     */
    public void setComparator(GeometryComparator comparator) {
        if (this.comparator != comparator) {
            this.comparator = comparator;
            updateFlag = true;
        }
    }
    /**
     * Set the camera that will be set on the geometry comparators
     * via {@link GeometryComparator#setCamera(com.jme3.renderer.Camera)}.
     *
     * @param cam Camera to use for sorting.
     */
    public void setCamera(Camera cam) {
        if (this.cam != cam) {
            this.cam = cam;
            comparator.setCamera(this.cam);
            updateFlag = true;
        }
        for (GeometryQueue q : internalQueues) {
            q.setCamera(cam);
        }
    }
    /**
     * Sets the depth range geometries in this queue (not internal queues)
     * are rendered at.
     * 
     * @param depth 
     */
    public void setDepth(DepthRange depth) {
        this.depth.set(depth);
    }
    /**
     * Sets this queue (not internal queues) to render in perspective mode
     * (as opposed to parallel projection or orthogonal).
     * 
     * @param perspective 
     */
    public void setPerspective(boolean perspective) {
        this.perspective = perspective;
    }

    /**
     * Returns the GeometryComparator that this Geometry list uses
     * for sorting.
     *
     * @return the pre-existing instance
     */
    public GeometryComparator getComparator() {
        return comparator;
    }
    /**
     * Gets the list of internal queues.
     * <p>
     * Do not modify.
     * 
     * @return 
     */
    public LinkedList<GeometryQueue> getInternalQueues() {
        return internalQueues;
    }
    /**
     * Returns the number of elements in this GeometryList.
     *
     * @return Number of elements in the list
     */
    public int size() {
        return size;
    }
    /**
     * Gets the number of geometries contained in this queue and internal queues.
     * 
     * @return 
     */
    public int getNumGeometries() {
        int s = size;
        for (GeometryQueue q : internalQueues) {
            s += q.getNumGeometries();
        }
        return s;
    }
    /**
     * Returns the element at the given index.
     *
     * @param index The index to lookup
     * @return Geometry at the index
     */
    public Geometry get(int index) {
        return geometries[index];
    }
    /**
     * 
     * @return 
     */
    public DepthRange getDepth() {
        return depth;
    }
    /**
     * 
     * @return 
     */
    public boolean isPerspective() {
        return perspective;
    }
    /**
     * Returns true if this queue or any internal queue contains geometry.
     * 
     * @return 
     */
    public boolean containsGeometry() {
        if (size > 0) return true;
        for (GeometryQueue q : internalQueues) {
            if (q.containsGeometry()) return true;
        }
        return false;
    }
    /**
     * 
     * @return 
     */
    public int getAllocatedSpace() {
        int space = geometries.length;
        for (GeometryQueue q : internalQueues) {
            space += q.getAllocatedSpace();
        }
        return space;
    }

    @Override
    public Iterator<Geometry> iterator() {
        return new GeometryIterator();
    }
    
    private class GeometryIterator implements Iterator<Geometry> {
        
        private int index = 0;
        private Iterator<GeometryQueue> queue;
        private Iterator<Geometry> queueGeom;
        
        @Override
        public boolean hasNext() {
            if (index < size || (queueGeom != null && queueGeom.hasNext())) {
                return true;
            }
            if (queue == null) {
                queue = internalQueues.iterator();
            }
            while (queueGeom == null || !queueGeom.hasNext()) {
                if (queue.hasNext()) {
                    queueGeom = queue.next().iterator();
                } else {
                    return false;
                }
            }
            return true;
        }

        @Override
        public Geometry next() {
            if (index < size) {
                return geometries[index++];
            }
            return queueGeom.next();
        }
        
    }
    
}
