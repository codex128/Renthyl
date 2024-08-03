/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.framegraph.material;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.material.TechniqueDef;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author codex
 */
public class MaterialAdapter {
    
    private final HashMap<String, String> adapters = new HashMap<>();
    
    public void add(String matdefName, String techniqueName) {
        adapters.put(matdefName, techniqueName);
    }
    
    public void adaptAll(AssetManager assetManager) {
        for (String m : adapters.keySet()) {
            MaterialDef matdef = assetManager.loadAsset(new AssetKey<>(m));
            RawTechnique raw = assetManager.loadAsset(new AssetKey<>(adapters.get(m)));
            System.out.println("adapting "+m);
            raw.apply(matdef);
        }
    }
    public boolean adaptMaterial(AssetManager assetManager, Material material, String requiredTechnique) {
        MaterialDef matdef = material.getMaterialDef();
        List<TechniqueDef> techs = matdef.getTechniqueDefs(requiredTechnique);
        if (techs != null && !techs.isEmpty()) {
            return true;
        }
        String techFile = adapters.get(matdef.getAssetName());
        if (techFile == null) {
            return false;
        }
        RawTechnique raw = assetManager.loadAsset(new AssetKey<>(techFile));
        raw.setName(requiredTechnique);
        raw.apply(matdef);
        return true;
    }
    
    public static void injectTechnique(AssetManager assetManager, String matdefName, String techName) {
        MaterialDef matdef = assetManager.loadAsset(new AssetKey<>(matdefName));
        RawTechnique technique = assetManager.loadAsset(new AssetKey<>(techName));
        technique.apply(matdef);
    }
    
}
