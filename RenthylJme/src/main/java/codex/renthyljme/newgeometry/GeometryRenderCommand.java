package codex.renthyljme.newgeometry;

import codex.renthyljme.utils.MaterialUtils;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.TechniqueDef;
import com.jme3.math.Matrix4f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.scene.Geometry;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public class GeometryRenderCommand {

    private final Geometry geometry;
    private final Material material;
    private final String bucket;
    private final String technique;
    private final RenderState state;
    private final LightList lights;
    private final int shaderSortId;
    private final int textureSortId;

    public GeometryRenderCommand(Geometry geometry) {
        this(geometry, geometry.getMaterial(), geometry.getQueueBucket().name(), null, null, null);
    }

    public GeometryRenderCommand(Geometry geometry, String bucket) {
        this(geometry, geometry.getMaterial(), bucket, null, null, null);
    }

    public GeometryRenderCommand(Geometry geometry, Material material) {
        this(geometry, material, geometry.getQueueBucket().name(), null, null, null);
    }

    public GeometryRenderCommand(Geometry geometry, Material material, String bucket, String technique, RenderState state, LightList lights) {
        this.geometry = geometry;
        this.material = material;
        this.bucket = bucket;
        this.technique = technique != null ? technique : TechniqueDef.DEFAULT_TECHNIQUE_NAME;
        this.state = state;
        this.lights = lights;
        this.shaderSortId = MaterialUtils.getShaderSortId(material, this.technique);
        this.textureSortId = MaterialUtils.computeTextureSortId(material);
        
    }

    public void render(RenderManager rm) {
        rm.setWorldMatrix(geometry.isIgnoreTransform() ? Matrix4f.IDENTITY : geometry.getWorldMatrix());
        rm.setForcedRenderState(state);
        material.selectTechnique(technique, rm);
        if (lights == null) {
            material.render(geometry, rm);
        } else {
            material.render(geometry, lights, rm);
        }
    }

    public boolean intersectsFrustum(Camera camera) {
        return camera.contains(geometry.getWorldBound()) != Camera.FrustumIntersect.Outside;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public Material getMaterial() {
        return material;
    }

    public String getTechnique() {
        return technique;
    }

    public RenderState getState() {
        return state;
    }

    public String getBucket() {
        return bucket;
    }

    public int getShaderSortId() {
        return shaderSortId;
    }

    public int getTextureSortId() {
        return textureSortId;
    }

}
