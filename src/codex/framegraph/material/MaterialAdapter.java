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
