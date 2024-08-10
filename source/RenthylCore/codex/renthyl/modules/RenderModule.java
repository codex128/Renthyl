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
package codex.renthyl.modules;

import codex.renthyl.Connectable;
import codex.renthyl.ExecutionQueueList;
import codex.renthyl.FGRenderContext;
import codex.renthyl.FrameGraph;
import codex.renthyl.resources.ResourceTicket;
import codex.renthyl.resources.ResourceUser;
import codex.renthyl.resources.TicketGroup;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 * @author codex
 */
public abstract class RenderModule implements Connectable, ResourceUser, Savable {
    
    protected FrameGraph frameGraph;
    protected String name = "RenderModule";
    protected RenderContainer parent;
    protected final ModuleIndex index = new ModuleIndex();
    protected final LinkedList<ResourceTicket> inputs = new LinkedList<>();
    protected final LinkedList<ResourceTicket> outputs = new LinkedList<>();
    protected final HashMap<String, TicketGroup> groups = new HashMap<>();
    private int refs = 0;
    private int id = -1;
    
    @Override
    public LinkedList<ResourceTicket> getInputTickets() {
        return inputs;
    }
    @Override
    public LinkedList<ResourceTicket> getOutputTickets() {
        return outputs;
    }
    @Override
    public ModuleIndex getIndex() {
        return index;
    }
    @Override
    public void countReferences() {
        refs = outputs.size();
    }
    @Override
    public void dereference() {
        if (--refs < 0) {
            throw new IllegalStateException("Cannot dereference unreferenced module.");
        }
    }
    @Override
    public boolean isUsed() {
        return refs > 0;
    }
    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(name, "name", "RenderModule");
        out.write(id, "exportId", -1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        name = in.readString("name", "RenderModule");
        id = in.readInt("exportId", -1);
    }
    
    @Override
    public ResourceTicket getInput(String name) {
        return getTicketFromStream(inputs.stream(), name);
    }
    @Override
    public ResourceTicket getOutput(String name) {
        return getTicketFromStream(outputs.stream(), name);
    }
    @Override
    public TicketGroup getGroup(String name) {
        return groups.get(name);
    }
    @Override
    public ResourceTicket addTicketListEntry(String groupName) {
        TicketGroup g = getGroup(name, true);
        g.requireAsList(true);
        return addInput(g.add());
    }
    @Override
    public void setLayoutUpdateNeeded() {
        if (isAssigned()) {
            frameGraph.setLayoutUpdateNeeded();
        }
    }
    
    public int getId() {
        return id;
    }
    
    /**
     * Adds the ticket as input.
     * 
     * @param <T>
     * @param input
     * @return given ticket
     */
    public <T> ResourceTicket<T> addInput(ResourceTicket<T> input) {
        getInputTickets().add(input);
        return input;
    }
    /**
     * Adds the ticket as output.
     * 
     * @param <T>
     * @param output
     * @return given ticket
     */
    public <T> ResourceTicket<T> addOutput(ResourceTicket<T> output) {
        getOutputTickets().add(output);
        return output;
    }
    /**
     * Creates and registers a new ticket as input.
     * 
     * @param <T>
     * @param name name assigned to the new ticket
     * @return created ticket
     */
    public <T> ResourceTicket<T> addInput(String name) {
        ResourceTicket.validateUserTicketName(name);
        return addInput(new ResourceTicket<>(name));
    }
    /**
     * Creates and registers a new ticket as output.
     * 
     * @param <T>
     * @param name name assigned to the new ticket
     * @return created ticket
     */
    public <T> ResourceTicket<T> addOutput(String name) {
        ResourceTicket.validateUserTicketName(name);
        return addOutput(new ResourceTicket<>(name));
    }
    /**
     * Creates and adds a ticket array as a group input of the specified length under the given name.
     * <p>
     * A group bundles several tickets together so that they can easily be used together.
     * Each individual ticket is handled just like any other, it is just registered with the group as well.
     * Names are formatted as {@code groupName+"["+index+"]"}.
     * 
     * @param <T>
     * @param name
     * @param length
     * @return created ticket array
     */
    public <T> ResourceTicket<T>[] addInputGroup(String name, int length) {
        ResourceTicket.validateUserTicketName(name);
        TicketGroup group = new TicketGroup(name, length);
        for (int i = 0; i < length; i++) {
            group.getArray()[i] = addInput(group.create(i));
        }
        groups.put(name, group);
        return group.getArray();
    }
    /**
     * Creates and adds a ticket array as a group output of the specified length under the given name.
     * <p>
     * A group bundles several tickets together so that they can easily be used together.
     * Each individual ticket is handled just like any other, it is just registered with the group as well.
     * Names are formatted as {@code groupName+"["+index+"]"}.
     * 
     * @param <T>
     * @param name
     * @param length
     * @return create ticket array
     */
    public <T> ResourceTicket<T>[] addOutputGroup(String name, int length) {
        ResourceTicket.validateUserTicketName(name);
        TicketGroup group = new TicketGroup(name, length);
        for (int i = 0; i < length; i++) {
            group.getArray()[i] = addOutput(group.create(i));
        }
        groups.put(name, group);
        return group.getArray();
    }
    /**
     * Creates an input ticket list.
     * <p>
     * A ticket list is an extension of a ticket group where the size is indefinite, meaning connections
     * can be added or removed at will. The order of connections is not guaranteed, especially
     * when a ticket list is loaded from a save file.
     * <p>
     * Each addition or removal from a ticket list requires an array resize, so it a ticket
     * list should remain static where possible.
     * 
     * @param name 
     */
    public void addInputList(String name) {
        ResourceTicket.validateUserTicketName(name);
        groups.put(name, new TicketGroup(name));
    }
    
    private static ResourceTicket getTicketFromStream(Stream<ResourceTicket> stream, String name) {
        return stream.filter(t -> name.equals(t.getName())).findFirst().orElse(null);
    }
    protected ResourceTicket[] getGroupArray(String name) {
        return getGroup(name, true).getArray();
    }
    
    /**
     * Initializes this module to the FrameGraph.
     * 
     * @param frameGraph 
     * @throws IllegalStateException if module is already initialized to a FrameGraph.
     */
    public void initializeModule(FrameGraph frameGraph) {
        if (this.frameGraph != null) {
            throw new IllegalStateException("Module already initialized.");
        }
        this.frameGraph = frameGraph;
        this.frameGraph.setLayoutUpdateNeeded();
        id = this.frameGraph.getNextId();
        initModule(this.frameGraph);
    }
    /**
     * Updates this module's index from the supplier.
     * 
     * @param context
     * @param queues
     * @param parentThread
     */
    public void queueModule(FGRenderContext context, ExecutionQueueList queues, int parentThread) {
        index.set(queues.add(this, parentThread));
    }
    /**
     * Executes this module.
     * 
     * @param context 
     */
    public void executeModuleRender(FGRenderContext context) {
        if (!isUsed()) {
            return;
        }
        executeRender(context);
    }
    /**
     * Resets this module from execution.
     * 
     * @param context 
     */
    public void resetModuleRender(FGRenderContext context) {
        resetRender(context);
    }
    /**
     * Cleans up this module from being attached to a FrameGraph.
     */
    public void cleanupModule() {
        id = -1;
        if (frameGraph != null) {
            cleanupModule(frameGraph);
            for (ResourceTicket t : inputs) {
                t.setSource(null);
            }
            for (ResourceTicket t : outputs) {
                t.clearAllTargets();
            }
            frameGraph.setLayoutUpdateNeeded();
            frameGraph = null;
        }
    }
    
    /**
     * Initializes the RenderModule implementation.
     * <p>
     * For most cases, use {@link #initializeModule(com.jme3.renderer.framegraph.FrameGraph)}
     * instead.
     * 
     * @param frameGraph 
     */
    protected abstract void initModule(FrameGraph frameGraph);
    /**
     * Prepares the RenderModule implementation.
     * <p>
     * For most cases, use {@link #prepareModuleRender(com.jme3.renderer.framegraph.FGRenderContext, com.jme3.renderer.framegraph.PassIndex)}
     * instead.
     * 
     * @param context 
     */
    protected abstract void prepareModuleRender(FGRenderContext context);
    /**
     * Executes the RenderModule implementation.
     * <p>
     * For most cases, use {@link #executeModuleRender(com.jme3.renderer.framegraph.FGRenderContext)}
     * instead.
     * 
     * @param context 
     */
    protected abstract void executeRender(FGRenderContext context);
    /**
     * Resets the RenderModule after execution.
     * 
     * @param context 
     */
    protected abstract void resetRender(FGRenderContext context);
    /**
     * Cleans up the RenderModule implementation.
     * <p>
     * For most cases, use {@link #cleanupModule()} instead.
     * 
     * @param frameGraph 
     */
    protected abstract void cleanupModule(FrameGraph frameGraph);
    
    /**
     * Called when all rendering is complete in a render frame this
     * module participated in (regardless of culling).
     */
    public abstract void renderingComplete();
    /**
     * Traverses this module.
     * 
     * @param traverser 
     */
    public abstract void traverse(Consumer<RenderModule> traverser);
    
    /**
     * Sets the name of this module.
     * 
     * @param name 
     */
    public void setName(String name) {
        assert name != null : "Name cannot be null.";
        assert !name.isEmpty() : "Name cannot be an empty string.";
        this.name = name;
    }
    /**
     * Sets the name of this module.
     * 
     * @param name
     * @param preserveProjectName if true, the project name appended to the
     * current name (if any) will be preserved into the resulting name.
     */
    public void setName(String name, boolean preserveProjectName) {
        if (preserveProjectName) {
            String[] array = this.name.split(":", 2);
            if (array.length > 1) {
                setName(array[0]+":"+name);
                return;
            }
        }
        setName(name);
    }
    /**
     * Appends the project name to the beginning of the current name.
     * <p>
     * The resulting name is formatted as {@code "[projectName]:[currentName]"}.
     * If the current name already has a project name appended (denoted by ':'),
     * the given project name will override that.
     * <p>
     * This can be used to ensure that modules between "projects" included in
     * the framegraph do not have duplicate names. For example, if importing
     * a bloom effect project, set the project name to "Bloom" so that modules
     * in the project can be referenced without fear of accidentally referencing
     * something else.
     * 
     * @param projectName 
     */
    public void appendProjectName(String projectName) {
        String[] array = name.split(":", 2);
        if (array.length == 1) {
            setName(projectName+":"+name);
        } else {
            setName(projectName+":"+array[1]);
        }
    }
    /**
     * Sets the parent of this module.
     * 
     * @param parent
     * @return 
     */
    protected boolean setParent(RenderContainer parent) {
        this.parent = parent;
        return true;
    }
    
    /**
     * 
     * @return 
     */
    public String getName() {
        return name;
    }
    /**
     * Gets the project name appended to the current name, if any.
     * 
     * @return project name, or null
     */
    public String getProjectName() {
        String[] array = name.split(":");
        if (array.length > 1) {
            return array[0];
        } else {
            return null;
        }
    }
    /**
     * 
     * @return 
     */
    public RenderContainer getParent() {
        return parent;
    }
    /**
     * Returns true if this module is assigned to a FrameGraph.
     * 
     * @return 
     */
    public boolean isAssigned() {
        return frameGraph != null;
    }
    /**
     * Returns true if this module runs on a thread other than the main thread.
     * 
     * @return 
     */
    public boolean isAsync() {
        return !index.isMainThread();
    }
    
}
