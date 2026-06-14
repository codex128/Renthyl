package codex.renthyljme.shadowsnew;

import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class ShadowMask {

    private final GLComputeShader shader;
    private final WorkSize work = new WorkSize();
    private final List<Light> lights = new ArrayList<>();
    private final TextureImage image;
    private Matrix4f viewProjInverse;
    private Texture2D map;
    private boolean overwrite = true;

    public ShadowMask(AssetManager assetManager, Camera view, Texture2D map) {
        this.viewProjInverse = view.getViewProjectionMatrix().invert();
        this.image = new TextureImage(map, TextureImage.Access.ReadWrite);
        setMap(map);
        shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/Shadows/ShadowCompose.glsl");
        shader.uniformTexture("SceneDepthMap");
        shader.uniformTexture("SceneNormalsMap");
        shader.uniformTexture("ShadowMap");
        shader.uniformMatrix4("CamViewProjectionInverse");
        shader.uniformMatrix4("LightViewProjectionMatrix");
        shader.uniformInt("LightType");
        shader.uniformInt("LightIndex");
        shader.uniformVector2("LightRange");
        shader.uniformVector3("LightPosition");
        shader.uniformBoolean("Overwrite");
        shader.uniformImage("Contribution");
    }

    public void composeLight(Texture2D sceneDepth, Texture2D shadowMap, Matrix4f lightViewProj) {
        if (lights.isEmpty()) {
            throw new IllegalStateException("No lights added.");
        }
        shader.set("SceneDepthMap", sceneDepth);
        shader.set("Contribution", image);
        shader.set("CamViewProjectionInverse", viewProjInverse);
        shader.set("ShadowMap", shadowMap);
        shader.set("LightViewProjectionMatrix", lightViewProj);
        shader.set("LightType", lights.getLast().getType().getId());
        shader.set("LightIndex", lights.size() - 1);
        shader.set("Overwrite", overwrite);
        shader.execute(work);
        overwrite = false;
    }

    public void addLight(Light light) {
        lights.add(light);
    }

    public void reset() {
        lights.clear();
        overwrite = true;
    }

    public int indexOfLight(Light light) {
        return lights.indexOf(light);
    }

    public void setViewProjInverse(Matrix4f viewProjInverse) {
        this.viewProjInverse = viewProjInverse;
    }

    public void setMap(Texture2D map) {
        if (this.map != map) {
            image.setTexture(map);
            work.clear().setGlobal(map.getImage().getWidth(), map.getImage().getHeight(), 1).shiftToLocal(2).clearZ();
        }
        this.map = map;
    }

    public Texture2D getMap() {
        return map;
    }

    public List<Light> getLights() {
        return lights;
    }

}
