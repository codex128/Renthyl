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
package codex.framegraph.modules;

import codex.framegraph.FGRenderContext;
import codex.framegraph.FrameGraph;
import codex.framegraph.IndexSupplier;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 * @author codex
 * @param <R>
 */
public class RenderContainer <R extends RenderModule> extends RenderModule implements Iterable<R> {

    protected final ArrayList<R> queue = new ArrayList<>();
    
    @Override
    public void initModule(FrameGraph frameGraph) {
        for (RenderModule m : queue) {
            m.initializeModule(frameGraph);
        }
    }
    @Override
    public void cleanupModule(FrameGraph frameGraph) {
        for (RenderModule m : queue) {
            m.cleanupModule();
        }
    }
    @Override
    public void updateModuleIndex(IndexSupplier supplier) {
        supplier.getNextInQueue(index);
        for (RenderModule m : queue) {
            m.updateModuleIndex(supplier);
        }
    }
    @Override
    public void prepareModuleRender(FGRenderContext context) {
        for (RenderModule m : queue) {
            m.prepareModuleRender(context);
        }
    }
    @Override
    public void executeRender(FGRenderContext context) {
        for (RenderModule m : queue) {
            if (isInterrupted()) {
                break;
            }
            m.executeModuleRender(context);
        }
    }
    @Override
    public void resetRender(FGRenderContext context) {
        for (RenderModule m : queue) {
            m.resetRender(context);
        }
    }
    @Override
    public void renderingComplete() {
        for (RenderModule m : queue) {
            m.renderingComplete();
        }
    }
    @Override
    public void countReferences() {
        for (RenderModule m : queue) {
            m.countReferences();
        }
    }
    @Override
    public boolean isUsed() {
        // if executing a container becomes heavy on its own, change this to
        // check isUsed() for each contained module.
        return !queue.isEmpty();
    }
    @Override
    public void interrupt() {
        super.interrupt();
        for (RenderModule m : queue) {
            m.interrupt();
        }
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule out = ex.getCapsule(this);
        ArrayList<RenderModule> array = new ArrayList<>();
        array.addAll(queue);
        out.writeSavableArrayList(array, "queue", new ArrayList<>(0));
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        ArrayList<R> array = in.readSavableArrayList("queue", new ArrayList<>(0));
        queue.addAll(array);
    }
    @Override
    public Iterator<R> iterator() {
        return queue.iterator();
    }
    @Override
    public void traverse(Consumer<RenderModule> traverser) {
        traverser.accept(this);
        for (RenderModule m : queue) {
            m.traverse(traverser);
        }
    }
    
    public <T extends R> T add(T module, int index) {
        assert module != null : "Cannot add null module.";
        assert this != module : "Cannot add container to itself.";
        if (module.getParent() != null) {
            module.getParent().remove(module);
        }
        if (index < 0) {
            index = queue.size();
        }
        queue.add(index, module);
        if (module.setParent(this)) {
            if (isAssigned()) {
                module.initializeModule(frameGraph);
            }
            return module;
        }
        throw new IllegalArgumentException(module+" cannot be added to this container.");
    }
    public <T extends R> T add(T module) {
        return add(module, queue.size());
    }
    public <T extends R> T[] addLoop(T[] array, int start, Function<Integer, T> factory, String source, String target) {
        for (int i = 0; i < array.length; i++) {
            T module = array[i];
            if (module == null) {
                if (factory == null) {
                    throw new NullPointerException("Module to add cannot be null.");
                }
                module = array[i] = factory.apply(i);
            }
            if (start >= 0) {
                add(module, start++);
            } else {
                add(module);
            }
            if (i > 0) {
                array[i].makeInput(array[i-1], source, target);
            }
        }
        return array;
    }
    public R get(int index) {
        return queue.get(index);
    }
    public <T extends RenderModule> T get(ModuleLocator<T> by) {
        for (RenderModule m : queue) {
            T module = by.accept(m);
            if (module != null) {
                return module;
            } else if (m instanceof RenderContainer) {
                module = (T)((RenderContainer)m).get(by);
                if (module != null) {
                    return module;
                }
            }
        }
        return null;
    }
    public boolean remove(R module) {
        if (module.getParent() == this && queue.remove(module)) {
            module.setParent(null);
            module.cleanupModule();
            return true;
        }
        return false;
    }
    public R remove(int index) {
        if (index < 0 || index >= queue.size()) {
            return null;
        }
        R m = queue.remove(index);
        m.setParent(null);
        m.cleanupModule();
        return m;
    }
    public void clear() {
        for (RenderModule m : queue) {
            m.cleanupModule();
        }
        queue.clear();
    }
    
    public int size() {
        return queue.size();
    }
    
}
