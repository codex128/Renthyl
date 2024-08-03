/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.material;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.material.TechniqueDef;
import com.jme3.util.blockparser.BlockLanguageParser;
import com.jme3.util.blockparser.Statement;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import jme3tools.shader.Preprocessor;

/**
 *
 * @author codex
 */
public class TechniqueLoader implements AssetLoader {
    
    private RawTechnique technique;
    
    @Override
    public Object load(AssetInfo info) throws IOException {
        InputStream in = info.openStream();
        technique = null;
        try {
            in = Preprocessor.apply(in);
            load(BlockLanguageParser.parse(in).get(0));
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return technique;
    }
    
    private void load(Statement root) throws IOException {
        String name = root.getLine();
        if (!name.startsWith("Technique")) {
            throw new IOException("File must start by declaring a technique.");
        }
        int length = "Technique".length() + 1;
        if (name.length() >= length) {
            name = name.substring(length).trim();
        } else {
            name = TechniqueDef.DEFAULT_TECHNIQUE_NAME;
        }
        LinkedList<ShaderInfo> shaders = new LinkedList<>();
        LinkedList<String> worldParams = new LinkedList<>();
        LinkedList<String> defines = new LinkedList<>();
        for (Statement content : root.getContents()) {
            String line = content.getLine().trim();
            if (line.contains("Shader")) {
                ShaderInfo info = ShaderInfo.fromStatement(line);
                shaders.add(info);
            } else if (line.startsWith("WorldParameters")) {
                for (Statement s : content.getContents()) {
                    worldParams.add(s.getLine().trim());
                }
            } else if (line.startsWith("Defines")) {
                for (Statement s : content.getContents()) {
                    defines.add(s.getLine().trim());
                }
            }
        }
        technique = new RawTechnique(name, shaders.toArray(new ShaderInfo[0]),
                worldParams.toArray(new String[0]), defines.toArray(new String[0]));
    }
    
}
