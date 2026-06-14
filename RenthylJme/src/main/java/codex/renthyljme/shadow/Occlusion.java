package codex.renthyljme.shadow;

import codex.renthyl.sockets.Socket;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyl.render.Renderable;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import com.jme3.bounding.BoundingBox;
import com.jme3.light.Light;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.texture.Texture2D;

import java.util.Collection;

public interface Occlusion <T extends Light> extends Renderable {

    ArgumentSocket<T> getLight();

    PointerSocket<ShadowMask> getShadowMask();

    PointerSocket<GeometryQueue> getOccluders();

    PointerSocket<GeometryQueue> getReceivers();
    
    static void computeShadowCameraBounds(Camera cam, BoundingBox box) {
        // not a very efficient implementation, but it will do for now
        Vector3f[] points = new Vector3f[8];
        points[0] = cam.getWorldCoordinates(Vector2f.ZERO, 0f);
        points[1] = cam.getWorldCoordinates(Vector2f.ZERO, 1f);
        points[2] = cam.getWorldCoordinates(new Vector2f(cam.getWidth(), 0f), 0f);
        points[3] = cam.getWorldCoordinates(new Vector2f(cam.getWidth(), 0f), 1f);
        points[4] = cam.getWorldCoordinates(new Vector2f(0f, cam.getHeight()), 0f);
        points[5] = cam.getWorldCoordinates(new Vector2f(0f, cam.getHeight()), 1f);
        points[6] = cam.getWorldCoordinates(new Vector2f(cam.getWidth(), cam.getHeight()), 0f);
        points[7] = cam.getWorldCoordinates(new Vector2f(cam.getWidth(), cam.getHeight()), 1f);
        Vector3f min = new Vector3f(points[0]);
        Vector3f max = new Vector3f(points[0]);
        for (Vector3f p : points) {
            min.x = Math.min(min.x, p.x);
            min.y = Math.min(min.y, p.y);
            min.z = Math.min(min.z, p.z);
            max.x = Math.max(max.x, p.x);
            max.y = Math.max(max.y, p.y);
            max.z = Math.max(max.z, p.z);
        }
        box.setMinMax(min, max);
    }

}
