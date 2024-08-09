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
 * Able to recieve inputs via input tickets and broadcast outputs
 * via output tickets.
 * 
 * @author codex
 */
public interface Connectable {
    
    /**
     * Gets the named input ticket.
     * 
     * @param name
     * @return named input ticket, or null
     */
    public ResourceTicket getInput(String name);
    
    /**
     * Gets the named output ticket.
     * 
     * @param name
     * @return named output ticket, or null
     */
    public ResourceTicket getOutput(String name);
    
    /**
     * Gets the named ticket group.
     * 
     * @param name
     * @return named ticket group, or null
     */
    public TicketGroup getGroup(String name);
    
    /**
     * Adds a new ticket to the named ticket group.
     * 
     * @param groupName ticket group to add to
     * @return new ticket
     */
    public ResourceTicket addTicketListEntry(String groupName);
    
    /**
     * Indicates that the FrameGraph layout has changed and requires
     * an update before rendering.
     */
    public void setLayoutUpdateNeeded();
    
    /**
     * Gets the named input ticket.
     * 
     * @param name
     * @param failOnMiss if true and the ticket does not exist, an exception is thrown
     * @return named input ticket, or null
     */
    public default ResourceTicket getInput(String name, boolean failOnMiss) {
        ResourceTicket t = getInput(name);
        if (t == null && failOnMiss) {
            throw new NullPointerException("Input ticket \""+name+"\" does not exist.");
        }
        return t;
    }
    
    /**
     * Gets the named output ticket.
     * 
     * @param name
     * @param failOnMiss if true and the ticket does not exist, an exception is thrown
     * @return named output ticket, or null
     */
    public default ResourceTicket getOutput(String name, boolean failOnMiss) {
        ResourceTicket t = getOutput(name);
        if (t == null && failOnMiss) {
            throw new NullPointerException("Output ticket \""+name+"\" does not exist.");
        }
        return t;
    }
    
    /**
     * Gets the named ticket group.
     * 
     * @param name
     * @param failOnMiss if ture and the group does not exist, an exception is thrown
     * @return named ticket group, or null
     */
    public default TicketGroup getGroup(String name, boolean failOnMiss) {
        TicketGroup g = getGroup(name);
        if (g == null && failOnMiss) {
            throw new NullPointerException("Group \""+name+"\" does not exist.");
        }
        return g;
    }
    
    /**
     * Connects the named source (output) ticket from the source Connectable to
     * the named target (input) ticket from this Connectable.
     * <p>
     * If either named ticket does not exist, an exception will be thrown.
     * 
     * @param source Connectable holding the named source ticket
     * @param sourceTicket name of the source ticket
     * @param targetTicket name of the target ticket
     */
    public default void makeInput(Connectable source, String sourceTicket, String targetTicket) {
        ResourceTicket out = source.getOutput(sourceTicket, true);
        if (TicketGroup.isListTicket(targetTicket)) {
            ResourceTicket t = addTicketListEntry(TicketGroup.extractGroupName(targetTicket));
            t.setSource(out);
        } else {
            ResourceTicket target = getInput(targetTicket, true);
            target.setSource(out);
        }
    }
    
    /**
     * Connects the named source (output) ticket group from the source Connectable
     * to the named target (input) ticket group from this Connectable.
     * <p>
     * Each source ticket in the source group connects starting at {@code sourceStart}
     * to the corresponding target ticket in the target group starting at {@code targetStart}.
     * No more than {@code length} tickets will be connected. This is similar to how
     * {@link System#arraycopy(java.lang.Object, int, java.lang.Object, int, int) System.arraycopy}
     * operates.
     * <p>
     * Length is clamped to the minimum of the ticket groups' sizes minus their corresponding start position.
     * 
     * @param source
     * @param sourceGroup
     * @param targetGroup
     * @param sourceStart
     * @param targetStart
     * @param length 
     */
    public default void makeGroupInput(Connectable source, String sourceGroup, String targetGroup, int sourceStart, int targetStart, int length) {
        ResourceTicket[] sourceArray = source.getGroup(sourceGroup, true).getArray();
        ResourceTicket[] targetArray = getGroup(targetGroup, true).getArray();
        int n = Math.min(sourceStart+length, sourceArray.length);
        int m = Math.min(targetStart+length, targetArray.length);
        for (; sourceStart < n && targetStart < m; sourceStart++, targetStart++) {
            targetArray[targetStart].setSource(sourceArray[sourceStart]);
        }
    }
    
    /**
     * Connects the named source (output) ticket group from the source Connectable
     * to the named target (input) ticket group from this Connectable.
     * <p>
     * Each source ticket in the source group connects to the corresponding target
     * ticket in the target group.
     * 
     * @param source
     * @param sourceGroup
     * @param targetGroup 
     */
    public default void makeGroupInput(Connectable source, String sourceGroup, String targetGroup) {
        makeGroupInput(source, sourceGroup, targetGroup, 0, 0, Integer.MAX_VALUE);
    }
    
    /**
     * Connects the named source (output) ticket from the source Connectable to
     * the named target (input) ticket group from this Connectable.
     * <p>
     * The target group must be a list (as opposed to an array). A new ticket is created
     * in the group for the source ticket to connect to.
     * 
     * @param source
     * @param sourceTicket
     * @param targetGroup 
     */
    public default void makeInputToList(Connectable source, String sourceTicket, String targetGroup) {
        ResourceTicket t = addTicketListEntry(targetGroup);
        t.setSource(source.getOutput(sourceTicket));
    }
    
    /**
     * Disconnects the named target (input) ticket from its source.
     * 
     * @param targetTicket 
     */
    public default void clearInput(String targetTicket) {
        getInput(targetTicket, true).setSource(null);
    }
    
    /**
     * Disconnects the named source (output) ticket from all dependents.
     * 
     * @param sourceTicket 
     */
    public default void disconnectOutput(String sourceTicket) {
        getOutput(sourceTicket, true).clearAllTargets();
    }
    
    /**
     * 
     * 
     * @param source
     * @param sourceTicket
     * @param targetGroup 
     */
    public default void disconnectFromGroup(Connectable source, String sourceTicket, String targetGroup) {
        getGroup(targetGroup, true).removeSource(source.getOutput(sourceTicket, true));
    }
    
}
