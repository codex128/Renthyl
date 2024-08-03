/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.material;

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
