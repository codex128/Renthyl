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
 * @param <T>
 */
public class TicketGroup <T> {
    
    /**
     * Prefix for tickets that are members of a list.
     */
    public static final String LIST = "#list:";
    
    private final String name;
    private ResourceTicket<T>[] array;
    private boolean list = false;

    public TicketGroup(String name) {
        this.name = name;
        this.array = new ResourceTicket[0];
        this.list = true;
    }
    public TicketGroup(String name, int length) {
        this.name = name;
        this.array = new ResourceTicket[length];
    }

    public ResourceTicket<T> create(int i) {
        String tName;
        if (!list) {
            tName = arrayTicketName(name, i);
        } else {
            tName = listTicketName(name);
        }
        return new ResourceTicket<>(tName);
    }

    public ResourceTicket<T> add() {
        ResourceTicket[] temp = new ResourceTicket[array.length+1];
        System.arraycopy(array, 0, temp, 0, array.length);
        array = temp;
        return (array[array.length-1] = create(array.length-1));
    }
    public int remove(ResourceTicket t) {
        requireAsList(true);
        int i = array.length-1;
        for (; i >= 0; i--) {
            if (array[i] == t) break;
        }
        if (i >= 0) {
            ResourceTicket[] temp = new ResourceTicket[array.length-1];
            if (i > 0) {
                System.arraycopy(array, 0, temp, 0, i);
            }
            if (i < array.length-1) {
                System.arraycopy(array, i+1, temp, i, array.length-i-1);
            }
            array = temp;
        }
        return i;
    }

    public void requireAsList(boolean list) {
        if (this.list != list) {
            throw new IllegalStateException("Group must be "+(list ? "a list" : "an array")+" in this context.");
        }
    }
    
    public String getName() {
        return name;
    }
    public ResourceTicket<T>[] getArray() {
        return array;
    }
    public boolean isList() {
        return list;
    }
    
    /**
     * 
     * @param group
     * @param i
     * @return 
     */
    public static String arrayTicketName(String group, int i) {
        return group+'['+i+']';
    }
    /**
     * 
     * @param group
     * @return 
     */
    public static String listTicketName(String group) {
        return LIST+group;
    }
    /**
     * 
     * @param name
     * @return 
     */
    public static boolean isListTicket(String name) {
        return name.startsWith(LIST);
    }
    /**
     * 
     * @param name
     * @return 
     */
    public static String extractGroupName(String name) {
        return name.substring(LIST.length());
    }
    
}
