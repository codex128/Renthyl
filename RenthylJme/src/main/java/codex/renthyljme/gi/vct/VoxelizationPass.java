/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyljme.gi.vct;

import codex.jmecompute.WorkSize;
import codex.jmecompute.assets.UniversalShaderLoader;
import codex.jmecompute.opengl.GLComputeShader;
import codex.jmecompute.opengl.Glsl;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.PointerSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyljme.FrameGraphContext;
import codex.renthyljme.definitions.FrameBufferDef;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.geometry.Visibility;
import codex.renthyljme.lights.LightBuffer;
import codex.renthyljme.render.CameraState;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.allocation.TemporalSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyljme.RasterTask;
import codex.renthyljme.geometry.GeometryRenderHandler;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import com.jme3.profile.AppProfiler;
import com.jme3.profile.SpStep;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
import com.jme3.texture.TextureImage;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.lwjgl.opengl.GL45;

/**
 *
 * @author codex
 */
public class VoxelizationPass extends RasterTask implements GeometryRenderHandler {

    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final TransitiveSocket<LightBuffer> lights = new TransitiveSocket<>(this);
    private final ArgumentSocket<ColorRGBA> ambient = new ArgumentSocket<>(this, ColorRGBA.Black);
    private final TransitiveSocket<Integer> gridSize = new TransitiveSocket<>(this);
    private final TransitiveSocket<BoundingBox> voxelBounds = new TransitiveSocket<>(this);
    private final TransitiveSocket<Texture3D> lightContribution = new TransitiveSocket<>(this);
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final AllocationSocket<Texture2D> renderTarget;
    private final TemporalSocket<Texture3D> voxels;
    private final FrameBufferDef bufferDef = new FrameBufferDef();
    private final TextureDef<Texture3D> voxelDef = TextureDef.texture3D(Image.Format.RGBA32F);
    private final TextureDef<Texture2D> renderTargetDef = TextureDef.texture2D();
    private final BoundingBox bound = new BoundingBox();
    private final Vector3f boundMin = new Vector3f();
    private final Vector3f boundMax = new Vector3f();
    private final Material material;
    private final GLComputeShader clear;
    private final GLComputeShader mipmapper;
    private final HashSet<Integer> mipsGenerated = new HashSet<>();
    private TextureImage voxelImg;
    private Camera camera;
    private float[] lightDataArray;

    public VoxelizationPass(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(geometry, lights, ambient, gridSize, voxelBounds, lightContribution);
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        renderTarget = addSocket(new AllocationSocket<>(this, allocator, renderTargetDef));
        voxels = addSocket(new TemporalSocket<>(this, allocator, voxelDef, 2));
        voxelDef.setMagFilter(Texture.MagFilter.Bilinear);
        voxelDef.setMinFilter(Texture.MinFilter.Trilinear);
        voxelDef.setWrap(Texture.WrapMode.EdgeClamp);
        clear = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/VXGI/voxelAssign.glsl", Glsl.V430);
        clear.uniformImage("VoxelMap");
        clear.uniformVector4("Value").set(Vector3f.ZERO);
        mipmapper = UniversalShaderLoader.loadComputeShader(assetManager, "RenthylJme/MatDefs/VXGI/voxelMipmap.glsl", Glsl.V430);
        mipmapper.uniformTexture("VoxelMap");
        mipmapper.uniformImage("TargetLevel");
        mipmapper.uniformInt("SourceLevel");
        material = new Material(assetManager, "RenthylJme/MatDefs/VXGI/voxelize.j3md");
        RenderState rs = material.getAdditionalRenderState();
        rs.setDepthTest(false);
        rs.setDepthWrite(false);
        rs.setFaceCullMode(RenderState.FaceCullMode.Off);
    }

    @Override
    protected void renderTask() {
        
        // add voxel grid size from source
        int n = gridSize.acquireOrThrow("Grid size required.");
        voxelDef.setCube(n);
        renderTargetDef.setSquare(n);
        
        // get voxel grid bounds
        voxelBounds.acquireOrThrow("Voxel bounds required.").clone(bound);
        bound.getMin(boundMin);
        bound.getMax(boundMax);
        
        // setup camera
        camera = context.getCamera().pushResize(camera, n, n, false);
        
        // acquireType framebuffer for rasterizing geometry into the voxel grid
        bufferDef.setColorTargets(renderTarget.acquire());
        FrameBuffer fbo = frameBuffer.acquire();
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();
        
        Texture3D voxelMap = voxels.getCurrent().acquire();
        if (voxelImg == null) {
            voxelImg = new TextureImage(voxelMap, TextureImage.Access.ReadWrite);
        } else {
            voxelImg.setTexture(voxelMap);
        }
        
        // clear the voxel grid
        clear.set("VoxelMap", voxelImg);
        clear.set("Value", Vector4f.ZERO);
        clear.execute(new WorkSize(n).shiftToLocal(2));
        
        // setup material
        LightBuffer lightData = lights.acquireOrThrow("Lights required.");
        lightDataArray = lightData.copyDataTo(lightDataArray);
        material.setParam("LightData", VarType.FloatArray, lightDataArray);
        material.setInt("LightDataSize", lightDataArray.length);
        material.setTexture("LightContributionMap", lightContribution.acquire());
        material.setColor("AmbientLight", ambient.acquire(ColorRGBA.Black));
        material.setParam("VoxelMap", VarType.Image3D, voxelImg);
        material.setVector3("GridMin", boundMin);
        material.setVector3("GridMax", boundMax);
        material.setInt("GridSize", n);
        //material.setTexture("TemporalVoxelMap", voxels.getSnapshot(1).acquire());
        context.getForcedMaterial().pushValue(material);
        
        // rasterize geometries into the voxel grid
        for (GeometryQueue q : geometry.acquire()) {
            q.render(context, this);
        }

        // generate mipmaps
        int voxId = voxelMap.getImage().getId();
        if (!mipsGenerated.contains(voxId)) {
            GL45.glBindTexture(GL45.GL_TEXTURE_3D, voxId);
            GL45.glGenerateMipmap(GL45.GL_TEXTURE_3D);
            mipsGenerated.add(voxId);
        }
        
        // populate mipmaps
        TextureImage target = new TextureImage(voxelMap, TextureImage.Access.WriteOnly);
        mipmapper.set("VoxelMap", voxelMap);
        mipmapper.set("TargetLevel", target);
        WorkSize work = new WorkSize();
        for (int i = 0; n > 4; i++) {
            AppProfiler profiler = context.getRenderManager().getProfiler();
            if (profiler != null) {
                profiler.appSubStep("Update voxel mipmap level " + i);
            }
            target.setLevel(i + 1);
            mipmapper.set("SourceLevel", i);
            mipmapper.execute(work.set((n = n >> 1) >> 2, 4));
        }

        context.getForcedMaterial().pop();
        context.getFrameBuffer().pop();
        context.getCamera().pop();
        
    }

    @Override
    public void renderGeometry(FrameGraphContext context, Geometry g) {
        // adapt material to geometry's pbr material
        transferParam(g, VarType.Vector4, "BaseColor", ColorRGBA.White);
        transferParam(g, VarType.Texture2D, "BaseColorMap", null);
        transferParam(g, VarType.Float, "AlphaDiscardThreshold", null);
        if (transferParam(g, VarType.Vector4, "Emissive", null) || transferParam(g, VarType.Texture2D, "EmissiveMap", null)) {
            transferParam(g, VarType.Float, "EmissivePower", 2f);
            transferParam(g, VarType.Float, "EmissiveIntensity", 5f);
        }
        context.getRenderManager().renderGeometry(g);
    }

    @Override
    public Visibility evaluateSpatialCulling(CameraState camera, Spatial spatial) {
        return Visibility.get(bound.intersects(spatial.getWorldBound()), true);
    }
    
    private boolean transferParam(Geometry g, VarType type, String paramName, Object defVal) {
        Object param = g.getMaterial().getParamValue(paramName);
        if (param != null) {
            material.setParam(paramName, type, param);
            return true;
        } else if (defVal != null) {
            material.setParam(paramName, type, defVal);
            return true;
        } else {
            material.clearParam(paramName);
            return false;
        }
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public PointerSocket<LightBuffer> getLights() {
        return lights;
    }

    public ArgumentSocket<ColorRGBA> getAmbient() {
        return ambient;
    }

    public PointerSocket<Integer> getGridSize() {
        return gridSize;
    }

    public PointerSocket<BoundingBox> getVoxelBounds() {
        return voxelBounds;
    }

    public PointerSocket<Texture3D> getLightContribution() {
        return lightContribution;
    }

    public Socket<Texture3D> getVoxels() {
        return voxels.getCurrent();
    }

}
