/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.material;

import com.jme3.shader.Shader;
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
    
    public static ShaderInfo fromStatement(String statement) {
        String[] file = statement.split(":");
        if (file.length != 2) {
            throw new IllegalStateException("Incorrect syntax.");
        }
        String[] args = file[0].split(whitespace);
        Shader.ShaderType type = Enum.valueOf(Shader.ShaderType.class, args[0].trim());
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
    
}
