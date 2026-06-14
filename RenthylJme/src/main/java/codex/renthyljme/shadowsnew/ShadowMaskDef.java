package codex.renthyljme.shadowsnew;

import codex.renthyl.definitions.ResourceDef;
import codex.renthyljme.definitions.TextureDef;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

public class ShadowMaskDef implements ResourceDef<ShadowMask> {

    private final AssetManager assetManager;
    private final TextureDef<Texture2D> mapDef = TextureDef.texture2D(Image.Format.R32UI);
    private Camera camera;

    public ShadowMaskDef(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    @Override
    public ShadowMask createResource() {
        mapDef.setSize(camera.getWidth(), camera.getHeight(), 1);
        return new ShadowMask(assetManager, camera, mapDef.createResource());
    }

    @Override
    public Float evaluateResource(Object resource) {
        mapDef.setSize(camera.getWidth(), camera.getHeight(), 1);
        if (resource instanceof ShadowMask) {
            ShadowMask m = (ShadowMask)resource;
            return mapDef.evaluateResource(m.getMap());
        } else {
            return ResourceDef.addEval(mapDef.evaluateResource(resource), 10f);
        }
    }

    @Override
    public ShadowMask conformResource(Object resource) {
        mapDef.setSize(camera.getWidth(), camera.getHeight(), 1);
        if (resource instanceof ShadowMask) {
            ShadowMask mask = (ShadowMask)resource;
            mask.setMap(mapDef.conformResource(mask.getMap()));
            mask.setViewProjInverse(camera.getViewProjectionMatrix().invert());
            mask.reset();
            return mask;
        } else {
            return new ShadowMask(assetManager, camera, mapDef.conformResource(resource));
        }
    }

    @Override
    public void dispose(ShadowMask object) {}

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public TextureDef<Texture2D> getMapDef() {
        return mapDef;
    }

    public Camera getCamera() {
        return camera;
    }

}
