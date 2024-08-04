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
package codex.framegraph;

/**
 * 
 * @author codex
 */
public interface Connectable {
    
    public ResourceTicket getInput(String name);
    
    public ResourceTicket getOutput(String name);
    
    public TicketGroup getGroup(String name);
    
    public ResourceTicket addListEntry(String groupName);
    
    public default ResourceTicket getInput(String name, boolean failOnMiss) {
        ResourceTicket t = getInput(name);
        if (t == null && failOnMiss) {
            throw new NullPointerException("Input ticket \""+name+"\" does not exist.");
        }
        return t;
    }
    
    public default ResourceTicket getOutput(String name, boolean failOnMiss) {
        ResourceTicket t = getOutput(name);
        if (t == null && failOnMiss) {
            throw new NullPointerException("Output ticket \""+name+"\" does not exist.");
        }
        return t;
    }
    
    public default TicketGroup getGroup(String name, boolean failOnMiss) {
        TicketGroup g = getGroup(name);
        if (g == null && failOnMiss) {
            throw new NullPointerException("Group \""+name+"\" does not exist.");
        }
        return g;
    }
    
    public default void makeInput(Connectable source, String sourceTicket, String targetTicket) {
        ResourceTicket out = source.getOutput(sourceTicket, true);
        if (TicketGroup.isListTicket(targetTicket)) {
            ResourceTicket t = addListEntry(TicketGroup.extractGroupName(targetTicket));
            t.setSource(out);
        } else {
            ResourceTicket target = getInput(targetTicket, true);
            target.setSource(out);
        }
    }
    
    public default void makeGroupInput(Connectable source, String sourceGroup, String targetGroup, int sourceStart, int targetStart, int length) {
        ResourceTicket[] sourceArray = source.getGroup(sourceGroup, true).getArray();
        ResourceTicket[] targetArray = getGroup(targetGroup, true).getArray();
        int n = Math.min(sourceStart+length, sourceArray.length);
        int m = Math.min(targetStart+length, targetArray.length);
        for (; sourceStart < n && targetStart < m; sourceStart++, targetStart++) {
            targetArray[targetStart].setSource(sourceArray[sourceStart]);
        }
    }
    
    public default void makeGroupInput(Connectable source, String sourceGroup, String targetGroup) {
        makeGroupInput(source, sourceGroup, targetGroup, 0, 0, Integer.MAX_VALUE);
    }
    
    public default void makeInputToList(Connectable source, String sourceTicket, String targetGroup) {
        ResourceTicket t = addListEntry(targetGroup);
        t.setSource(source.getOutput(sourceTicket));
    }
    
}
