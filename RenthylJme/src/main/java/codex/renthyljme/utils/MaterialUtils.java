package codex.renthyljme.utils;

import codex.renthyl.sockets.Socket;
import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.scene.Geometry;
import com.jme3.scene.SceneGraphIterator;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MaterialUtils {

    public static final String JME_PBR_LIGHTING = "Common/MatDefs/Light/PBRLighting.j3md";
    public static final String RENTHYL_PBR_LIGHTING = "RenthylJme/MatDefs/PBRLighting.j3md";

    /**
     * Migrates all materials in {@code scene} by creating a new material and transfering all
     * applicable parameters from the old material to the new. Specific material usage across
     * geometries is conserved.
     *
     * @param scene scene to upgrade materials of (not null)
     * @param filter selects the materials to upgrade (not null)
     * @param factory creates the upgraded materials (not null)
     */
    public static void migrateMaterials(Spatial scene, Predicate<Material> filter, Supplier<Material> factory) {
        Collection<MaterialMapping> mappings = new ArrayList<>();
        graph: for (Spatial s : new SceneGraphIterator(scene)) {
            if (s instanceof Geometry) {
                Material m = ((Geometry)s).getMaterial();
                if (!filter.test(m)) {
                    continue;
                }
                for (MaterialMapping map : mappings) {
                    if (map.original == m) {
                        s.setMaterial(map.upgrade);
                        continue graph;
                    }
                }
                MaterialMapping map = new MaterialMapping(m, factory.get());
                for (MatParam p : m.getParams()) {
                    if (parameterExists(map.upgrade, p.getName())) {
                        map.upgrade.setParam(p.getName(), p.getVarType(), p.getValue());
                    }
                }
                s.setMaterial(map.upgrade);
                mappings.add(map);
            }
        }
    }

    private static class MaterialMapping {
        public final Material original, upgrade;
        public MaterialMapping(Material original, Material upgrade) {
            this.original = original;
            this.upgrade = upgrade;
        }
    }

    /**
     * {@link #migrateMaterials(Spatial, Predicate, Supplier) Migrates} from
     * {@link #JME_PBR_LIGHTING} to {@link #RENTHYL_PBR_LIGHTING}.
     *
     * @param assetManager
     * @param scene
     */
    public static void migratePBRLighting(AssetManager assetManager, Spatial scene) {
        migrateMaterials(scene,
                m -> m.getMaterialDef().getAssetName().equals(JME_PBR_LIGHTING),
                () -> new Material(assetManager, RENTHYL_PBR_LIGHTING));
    }

    /**
     * Returns true if {@code parameter} exists as a parameter in the material.
     *
     * @param material
     * @param parameter
     * @return
     */
    public static boolean parameterExists(Material material, String parameter) {
        return material.getMaterialDef().getMaterialParam(parameter) != null;
    }

    /**
     * Sets the named parameter if it exists.
     *
     * @param material
     * @param name
     * @param value
     * @return true if the parameter exists
     */
    public static boolean setIfExists(Material material, String name, Object value) {
        if (parameterExists(material, name)) {
            material.setParam(name, value);
            return true;
        }
        return false;
    }

    public static boolean clearIfExists(Material material, String name) {
        if (parameterExists(material, name)) {
            material.clearParam(name);
            return true;
        }
        return false;
    }

    /**
     * Sets material parameters according to the elements in {@code parameters} by name.
     * Parameters not defined in {@code parameters} are left untouched.
     *
     * @param material
     * @param parameters
     * @param extract extracts the parameter to apply from the Object returned from {@code parameters}
     * @return the number of parameters set by this method
     */
    public static <T> int setParameters(Material material, Map<String, ? extends T> parameters, Function<T, Object> extract) {
        int count = 0;
        if (parameters.size() <= material.getMaterialDef().getMaterialParams().size()) {
            for (Map.Entry<String, ? extends T> p : parameters.entrySet()) if (parameterExists(material, p.getKey())) {
                material.setParam(p.getKey(), extract.apply(p.getValue()));
                count++;
            }
        } else for (MatParam m : material.getMaterialDef().getMaterialParams()) {
            T p = parameters.get(m.getName());
            if (p != null) {
                material.setParam(m.getName(), extract.apply(p));
                count++;
            }
        }
        return count;
    }

    /**
     * {@link Socket#acquire() Acquires} directly to the named material parameter. If
     * {@link Socket#acquire() acquire} returns null, the parameter is {@link Material#clearParam(String)
     * cleared}.
     *
     * @param material
     * @param name name of the material parameter
     * @param socket
     */
    public static <T> T acquireToMaterial(Material material, String name, Socket<T> socket) {
        T v = socket.acquire();
        if (v != null) {
            material.setParam(name, v);
        } else {
            material.clearParam(name);
        }
        return v;
    }

    public static int getShaderSortId(Material material, String technique) {
        List<TechniqueDef> techs = material.getMaterialDef().getTechniqueDefs(technique);
        if (techs.isEmpty()) {
            throw new IllegalArgumentException(technique + " is not a valid technique.");
        }
        return techs.get(0).getSortId();
    }

    /**
     * Evaluates the sorting ID based on the textures contained in the material.
     *
     * @param material
     * @return
     */
    public static int computeTextureSortId(Material material) {
        int sortId = 17;
        for (MatParam p : material.getParams()) {
            if (!p.getVarType().isTextureType()) {
                continue;
            }
            Texture t = (Texture)p.getValue();
            if (t == null || t.getImage() == null) {
                continue;
            }
            sortId = sortId * 23 + t.getImage().getId();
        }
        return sortId;
    }

}
