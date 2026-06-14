package codex.renthyljme.shadow;

import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.TextureImage;

public class ShadowMask {

    private final GLComputeShader shader;
    private final TextureImage mask;
    private final int maxLights;
    private final WorkSize work = new WorkSize();
    private Camera camera;
    private int nextMaskIndex = 0;

    public ShadowMask(AssetManager assetManager, Texture2D mask, int maxLights) {
        this.shader = createCompositorShader(assetManager);
        this.mask = new TextureImage(mask, TextureImage.Access.ReadWrite);
        this.maxLights = maxLights;
    }

    public int getNextMaskIndex() {
        if (nextMaskIndex >= maxLights) {
            return -1;
        }
        return nextMaskIndex++;
    }

    public void reset(Texture2D sceneDepth, Texture2D sceneNormals, Camera camera, String readNormals) {
        shader.set("SceneDepthMap", sceneDepth);
        shader.set("SceneNormalsMap", sceneNormals);
        shader.define("NORMALS", sceneNormals != null);
        shader.define("READ_NORMALS_LAMBDA", readNormals);
        shader.set("CamViewProjectionInverse", camera.getViewProjectionMatrix().invert());
        shader.set("Contribution", mask);
        shader.set("Overwrite", true);
        work.setGlobal(sceneDepth.getImage().getWidth(), sceneDepth.getImage().getHeight(), 1).setLocal(1).shiftToLocal(2).clearZ();
        this.camera = camera;
    }

    public void compose(Texture2D shadowMap, Matrix4f lightViewProjection, Light light, int maskIndex) {
        shader.set("ShadowMap", shadowMap);
        shader.set("LightViewProjectionMatrix", lightViewProjection);
        shader.set("LightType", light.getType());
        shader.set("LightIndex", maskIndex);
        uploadLightPosition(light);
        shader.execute(work);
        shader.set("Overwrite", false);
    }

    private void uploadLightPosition(Light l) {
        switch (l.getType()) {
            case Directional: {
                shader.set("LightPosition", ((DirectionalLight)l).getDirection());
            } break;
            case Point: {
                shader.set("LightPosition", ((PointLight)l).getPosition());
            } break;
            case Spot: {
                shader.set("LightPosition", ((SpotLight)l).getPosition());
            } break;
            default: throw new UnsupportedOperationException("Shadows for " + l.getType() + " lights are not supported.");
        }
    }

    public TextureImage getMask() {
        return mask;
    }

    public Camera getCamera() {
        return camera;
    }

    public int getMaxLights() {
        return maxLights;
    }

    public static GLComputeShader createCompositorShader(AssetManager assetManager) {
        GLComputeShader shader = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/Shadows/ShadowCompose.glsl");
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
        return shader;
    }

}
