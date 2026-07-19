package codex.renthyljme.shadow;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyljme.definitions.TextureDef;
import com.jme3.asset.AssetManager;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class ShadowMaskDef implements ResourceDef<ShadowMask> {

    private final AssetManager assetManager;
    private final TextureDef<Texture2D> mapDef = TextureDef.texture2D(Image.Format.R32UI);
    private int maxLights = 32;

    public ShadowMaskDef(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public ShadowMaskDef(AssetManager assetManager, int maxLights) {
        this.assetManager = assetManager;
        this.maxLights = maxLights;
    }

    @Override
    public ShadowMask createResource() {
        return new ShadowMask(assetManager, mapDef.createResource(), maxLights);
    }

    @Override
    public Float evaluateResource(Object resource) {
        if (resource instanceof ShadowMask) {
            ShadowMask m = (ShadowMask)resource;
            if (m.getMaxLights() < maxLights) {
                return null;
            }
            return mapDef.evaluateResource(m.getMask());
        }
        return null;
    }

    @Override
    public ShadowMask conformResource(Object resource) {
        return (ShadowMask)resource;
    }

    @Override
    public void dispose(ShadowMask object) {}

    public void setMaxLights(int maxLights) {
        assert maxLights > 0 : "Max lights must be positive.";
        this.maxLights = maxLights;
    }

    public TextureDef<Texture2D> getMapDef() {
        return mapDef;
    }

    public int getMaxLights() {
        return maxLights;
    }

}
