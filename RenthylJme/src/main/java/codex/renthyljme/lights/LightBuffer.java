package codex.renthyljme.lights;

import codex.jmecompute.opengl.GLComputeShader;
import codex.renthyljme.shadowsnew.ShadowMask;
import com.jme3.light.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.shader.VarType;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LightBuffer {

    public static final int FLOATS_PER_LIGHT = 12;

    private final FloatBuffer data;
    private final List<LightProbe> probes = new ArrayList<>();
    private final ColorRGBA ambient = new ColorRGBA();
    private final ColorRGBA tempColor = new ColorRGBA();

    public LightBuffer(int lightCapacity) {
        data = BufferUtils.createFloatBuffer(lightCapacity * FLOATS_PER_LIGHT);
    }

    public LightBuffer(FloatBuffer data) {
        this.data = data;
    }

    public void fill(Collection<Light> lights) {
        fill(lights, null);
    }

    public void fill(Collection<Light> lights, List<Light> shadowLights) {
        data.clear();
        probes.clear();
        ambient.set(0f, 0f, 0f, 0f);
        int i = 0;
        for (Light l : lights) {
            if (l.getType() == Light.Type.Ambient) {
                ambient.addLocal(l.getColor());
                continue;
            }
            if (l.getType() == Light.Type.Probe) {
                probes.add((LightProbe)l);
                continue;
            }
            if (!data.hasRemaining()) {
                System.out.println("Out of room in data buffer.");
                break;
            }
            int id = l.getType().getId();
            if (shadowLights != null) {
                if (id > 3) {
                    throw new IllegalStateException("Light type id is larger than two bits: cannot pack shadow indices.");
                }
                int shadowIndex = shadowLights.indexOf(l);
                if (shadowIndex >= 0) {
                    id += (shadowIndex + 1) << 2;
                }
            }
            putColor(tempColor.set(l.getColor()).setAlpha(id));
            switch (l.getType()) {
                case Directional: {
                    // 3 for color
                    // 1 for type and shadow index
                    // 3 for direction
                    // = 7 used elements = 5 unused elements
                    DirectionalLight dl = (DirectionalLight) l;
                    putVector(dl.getDirection());
                    //advance(5);
                } break;
                case Point: {
                    // 3 for color
                    // 1 for type and shadow index
                    // 3 for position
                    // 1 for inverse radius
                    // = 8 used elements = 4 unused elements
                    PointLight pl = (PointLight) l;
                    putVector(pl.getPosition());
                    data.put(pl.getInvRadius());
                    //advance(4);
                } break;
                case Spot: {
                    // 3 for color
                    // 1 for type and shadow index
                    // 3 for position
                    // 1 for inverse range
                    // 3 for direction
                    // 1 for cosine
                    // = 12 used elements = 0 unused elements
                    SpotLight sl = (SpotLight) l;
                    putVector(sl.getPosition());
                    data.put(sl.getInvSpotRange());
                    putVector(sl.getDirection());
                    data.put(sl.getPackedAngleCos());
                } break;
                default: throw new UnsupportedOperationException("Unsupported light type: " + l.getType());
            }
        }
        data.flip();
    }

    public float[] copyDataTo(float[] array) {
        if (array == null || data.limit() != array.length) {
            array = new float[data.limit()];
        }
        for (int i = 0; i < array.length; i++) {
            array[i] = data.get(i);
        }
        return array;
    }

    private FloatBuffer putVector(Vector3f vec) {
        return data.put(vec.x).put(vec.y).put(vec.z);
    }

    private FloatBuffer putColor(ColorRGBA color) {
        return data.put(color.r).put(color.g).put(color.b).put(color.a);
    }

    private FloatBuffer advance(int steps) {
        data.position(data.position() + steps);
        return data;
    }

    public void uploadProbes(Material material, int limit) {
        int i = 0;
        for (LightProbe p : probes) {
            if (i >= limit) {
                break;
            }
            String index = i == 0 ? "" : Integer.toString(i + 1);
            material.setTextureParam("g_PrevEnvMap" + index, VarType.TextureCubeMap, p.getPrefilteredEnvMap());
            material.setParam("g_ShCoeffs" + index, VarType.Vector3Array, p.getShCoeffs());
            material.setMatrix4("g_LightProbeData" + index, p.getUniformMatrix());
            i++;
        }
        throw new UnsupportedOperationException("Unable to set NB_PROBES define.");
    }

    public void uploadProbes(GLComputeShader shader, int limit) {
        int i = 0;
        for (LightProbe p : probes) {
            if (i >= limit) {
                break;
            }
            String index = i == 0 ? "" : Integer.toString(i + 1);
            shader.set("g_PrevEnvMap" + index, p.getPrefilteredEnvMap());
            shader.set("g_ShCoeffs" + index, p.getShCoeffs());
            shader.set("g_LightProbeData" + index, p.getUniformMatrix());
            i++;
        }
        shader.define("NB_PROBES", Math.min(probes.size(), limit));
    }

    public FloatBuffer getData() {
        return data;
    }

    public List<LightProbe> getProbes() {
        return probes;
    }

    public ColorRGBA getAmbient() {
        return ambient;
    }

    public int capacity() {
        return data.capacity() / FLOATS_PER_LIGHT;
    }

}
