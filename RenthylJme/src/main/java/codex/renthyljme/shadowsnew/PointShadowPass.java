package codex.renthyljme.shadowsnew;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyljme.render.CameraState;
import com.jme3.asset.AssetManager;
import com.jme3.light.PointLight;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class PointShadowPass extends AbstractShadowPass<PointLight> {

    private static final Vector3f[] directions = {Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z,
            Vector3f.UNIT_X.negate(), Vector3f.UNIT_Y.negate(), Vector3f.UNIT_Z.negate()};
    private static final Vector3f[] ups = {Vector3f.UNIT_Y, Vector3f.UNIT_Z, Vector3f.UNIT_Y,
            Vector3f.UNIT_Y, Vector3f.UNIT_Z, Vector3f.UNIT_Y};

    public PointShadowPass(AssetManager assetManager, ResourceAllocator allocator, int size) {
        super(assetManager, allocator, size);
        for (int i = 0; i < directions.length; i++) {
            CameraState c = new CameraState(new Camera(size, size), false);
            c.getCamera().setFrustumPerspective(90f, 1f, 0.1f, 2f);
            c.getCamera().lookAt(directions[i], ups[i]);
            cameras.add(c);
        }
    }

    @Override
    protected void updateCameraToLight(PointLight light, CameraState cam) {
        if (cam.getCamera().getFrustumFar() != light.getRadius() || !cam.getCamera().getLocation().equals(light.getPosition())) {
            cam.getCamera().setLocation(light.getPosition());
            cam.getCamera().setFrustumFar(light.getRadius());
        }
    }

}
