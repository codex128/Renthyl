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

import com.jme3.material.MatParam;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import com.jme3.material.plugins.J3MLoader;
import com.jme3.util.clone.Cloner;
import java.util.LinkedList;
import java.util.Set;

/**
 *
 * @author codex
 */
public class RawTechnique {
    
    private String name;
    private ShaderInfo[] shaders;
    private String[] worldParams;
    private String[] defines;

    public RawTechnique(String name, ShaderInfo[] shaders, String[] worldParams, String[] defines) {
        this.name = name;
        this.shaders = shaders;
        this.worldParams = worldParams;
        this.defines = defines;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setShaders(ShaderInfo... shaders) {
        this.shaders = shaders;
    }
    public void setWorldParams(String... worldParams) {
        this.worldParams = worldParams;
    }
    public void setDefines(String... defines) {
        this.defines = defines;
    }

    public String getName() {
        return name;
    }
    public ShaderInfo[] getShaders() {
        return shaders;
    }
    public String[] getWorldParams() {
        return worldParams;
    }
    public String[] getDefines() {
        return defines;
    }
    
    public TechniqueDef[] apply(MaterialDef matdef) {
        Cloner cloner = new Cloner();
        String uniqueName = matdef.getAssetName() + '@' + name;
        TechniqueDef def = new TechniqueDef(name, uniqueName.hashCode());
        for (String w : worldParams) {
            def.addWorldParam(w);
        }
        LinkedList<String> presetDefines = new LinkedList<>();
        for (String d : defines) {
            String[] args = d.split(":");
            switch (args.length) {
                case 1: presetDefines.add(args[0].trim()); break;
                case 2:
                    String paramName = args[1].trim();
                    MatParam param = matdef.getMaterialParam(paramName);
                    if (param == null) {
                        throw new NullPointerException("Material parameter \""+paramName+"\" does not exist.");
                    }
                    def.addShaderParamDefine(paramName, param.getVarType(), args[0].trim());
                    break;
                default:
                    throw new IllegalStateException("Incorrect syntax on technique define.");
            }
        }
        def.setShaderPrologue(J3MLoader.createShaderPrologue(presetDefines));
        def.createLogicFromLightMode();
        Set<Integer> versions = ShaderInfo.getVersions(shaders);
        TechniqueDef[] array = new TechniqueDef[versions.size()];
        int i = 0;
        for (int v : versions) {
            cloner.clearIndex();
            TechniqueDef td = array[i++] = cloner.clone(def);
            String version = ShaderInfo.stringVersion(v);
            for (ShaderInfo s : shaders) {
                td.addShaderFile(s.getType(), s.getFileName(), version);
            }
            matdef.addTechniqueDef(td);
        }
        return array;
    }
    
}
