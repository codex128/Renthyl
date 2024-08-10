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
package codex.renthyl.material;

import com.jme3.shader.Shader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author codex
 */
public class ShaderInfo {
    
    private static final String whitespace = "\\p{javaWhitespace}+";
    
    private final Shader.ShaderType type;
    private final String fileName;
    private final int[] versions;

    public ShaderInfo(Shader.ShaderType type, String fileName, int... versions) {
        this.type = type;
        this.fileName = fileName;
        this.versions = versions;
    }
    
    public Shader.ShaderType getType() {
        return type;
    }
    public String getFileName() {
        return fileName;
    }
    public int[] getVersions() {
        return versions;
    }
    
    @Override
    public String toString() {
        return "ShaderInfo[type="+type+", file=\""+fileName+"\", versions="+Arrays.toString(versions)+']';
    }
    
    public static ShaderInfo fromStatement(String statement) {
        String[] file = statement.split(":");
        if (file.length != 2) {
            throw new IllegalStateException("Incorrect syntax.");
        }
        String[] args = file[0].split(whitespace);
        Shader.ShaderType type = getShaderType(args[0].trim());
        int[] versions = new int[args.length-1];
        for (int i = 1; i < args.length; i++) {
            versions[i-1] = Integer.parseInt(args[i].trim().substring(4));
        }
        return new ShaderInfo(type, file[1].trim(), versions);
    }
    public static Set<Integer> getVersions(ShaderInfo... shaders) {
        HashSet<Integer> versions = new HashSet<>();
        for (ShaderInfo s : shaders) {
            for (int v : s.versions) {
                if (!versions.contains(v)) {
                    versions.add(v);
                }
            }
        }
        return versions;
    }
    public static String stringVersion(int version) {
        return "GLSL"+version;
    }
    
    private static Shader.ShaderType getShaderType(String arg) {
        int i = arg.indexOf("Shader");
        if (i <= 0) {
            throw new IllegalArgumentException(arg+" is not a valid shader type.");
        }
        arg = arg.substring(0, i);
        return Enum.valueOf(Shader.ShaderType.class, arg);
    }
    
}
