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
