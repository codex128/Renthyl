/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.shadow;

import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.*;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.RasterTask;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.texture.*;

import java.util.List;

/**
 *
 * @author gary
 */
public class ShadowMaskPass extends RasterTask {
    
    private static final int MAX_SHADOW_LIGHTS = 32;

    private final TransitiveSocket<Texture2D> sceneDepth = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture2D> sceneNormals = new TransitiveSocket<>(this);
    private final CollectorSocket<ShadowMap> shadowMaps = new CollectorSocket<>(this);
    private final ArgumentSocket<String> readNormalsLambda = new ArgumentSocket<>(this);
    private final AllocationSocket<Texture2D> shadowMask;
    private final ValueSocket<Light[]> lightShadowIndices = new ValueSocket<>(this);
    private final TextureDef<Texture2D> contributionDef = TextureDef.texture2D();
    private final GLComputeShader shader;
    private final WorkSize work = new WorkSize();
    private TextureImage resultImage;

    public ShadowMaskPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(sceneDepth, sceneNormals, shadowMaps, lightShadowIndices);
        shadowMask = addSocket(new AllocationSocket<>(this, allocator, contributionDef));
        contributionDef.setFormat(Image.Format.R32F);
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

    @Override
    protected void renderTask() {

        Texture2D depth = sceneDepth.acquireOrThrow("Scene depth required.");
        int w = depth.getImage().getWidth();
        int h = depth.getImage().getHeight();
        contributionDef.setSize(w, h);

        // normals
        Texture2D normals = sceneNormals.acquire();
        shader.set("SceneNormalsMap", normals);
        shader.define("NORMALS", normals != null); // workaround for uniform defines being broken
        if (normals != null) {
            shader.define("READ_NORMALS_LAMBDA", readNormalsLambda.acquire());
        }

        shader.set("SceneDepthMap", depth);
        shader.set("CamViewProjectionInverse", context.getViewPort().getCamera().getViewProjectionMatrix().invert());
        shader.set("Contribution", shadowMask.acquire());
        shader.set("Overwrite", true);

        // work size
        work.clear().setGlobal(w, h, 1).shiftToLocal(2).clearZ();

        // result image
        if (resultImage == null) {
            resultImage = new TextureImage(shadowMask.acquire(), TextureImage.Access.ReadWrite);
        } else {
            resultImage.setTexture(shadowMask.acquire());
        }
        shader.set("Contribution", resultImage);

        // render each shadow map to the composed image
        int nextIndex = 0;
        List<ShadowMap> maps = shadowMaps.acquire();
        Light[] indexMap = new Light[Math.min(maps.size(), MAX_SHADOW_LIGHTS)];
        lightShadowIndices.setValue(indexMap);
        for (ShadowMap m : maps) {
            if (m == null) {
                continue;
            }
            int i = indexOf(indexMap, m.getLight());
            if (i < 0 && nextIndex < MAX_SHADOW_LIGHTS) {
                indexMap[i = nextIndex++] = m.getLight();
            }
            if (i >= 0) {
                shader.set("ShadowMap", m.getMap());
                shader.set("LightViewProjectionMatrix", m.getProjection());
                shader.set("LightType", m.getLight().getType().getId());
                shader.set("LightIndex", i);
                shader.set("LightRange", m.getRange());
                if (normals != null) {
                    uploadLightPosition(m.getLight());
                }
                shader.execute(work);
                shader.set("Overwrite", false);
            }
        }
        
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
    
    private static int indexOf(Object[] array, Object obj) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == obj) {
                return i;
            }
        }
        return -1;
    }

    public PointerSocket<Texture2D> getSceneDepth() {
        return sceneDepth;
    }

    public PointerSocket<Texture2D> getSceneNormals() {
        return sceneNormals;
    }

    public CollectorSocket<ShadowMap> getShadowMaps() {
        return shadowMaps;
    }

    public ArgumentSocket<String> getReadNormalsLambda() {
        return readNormalsLambda;
    }

    public Socket<Texture2D> getShadowMask() {
        return shadowMask;
    }

    public Socket<Light[]> getLightShadowMapping() {
        return lightShadowIndices;
    }

}
