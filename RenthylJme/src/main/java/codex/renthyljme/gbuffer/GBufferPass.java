package codex.renthyljme.gbuffer;

import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.TransitiveSocket;
import codex.renthyl.sockets.collections.SocketMap;
import codex.renthyljme.FrameGraphContext;
import codex.renthyljme.definitions.FrameBufferDef;
import codex.renthyljme.definitions.TextureDef;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.geometry.GeometryRenderHandler;
import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.allocation.AllocationSocket;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyl.sockets.collections.CollectorSocket;
import codex.renthyl.sockets.collections.SocketList;
import codex.renthyljme.RasterTask;
import codex.renthyljme.render.RenderEnvironment;
import codex.renthyljme.utils.MaterialUtils;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GBufferPass extends RasterTask implements GeometryRenderHandler {

    private final ResourceAllocator allocator;
    private final CollectorSocket<GeometryQueue> geometry = new CollectorSocket<>(this);
    private final TransitiveSocket<Camera> camera = new TransitiveSocket<>(this);
    private final ArgumentSocket<RenderEnvironment> environment = new ArgumentSocket<>(this);
    private final Map<String, Object> parameterMap = new HashMap<>();
    private final SocketMap<String, ArgumentSocket<Object>, Object> parameters = new SocketMap<>(this, parameterMap);
    private final SocketList<DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D>, Texture2D> gbuffers = new SocketList<>(this);
    private final AllocationSocket<Texture2D> depth;
    private final AllocationSocket<FrameBuffer> frameBuffer;
    private final TextureDef<Texture2D> depthDef = TextureDef.texture2D(Image.Format.Depth);
    private final FrameBufferDef bufferDef = new FrameBufferDef();

    public GBufferPass(ResourceAllocator allocator) {
        this.allocator = allocator;
        addSockets(geometry, camera, environment, parameters, gbuffers);
        frameBuffer = addSocket(new AllocationSocket<>(this, allocator, bufferDef));
        depth = addSocket(new DefinedAllocationSocket<>(this, allocator, depthDef));
    }

    @Override
    protected void renderTask() {

        RenderEnvironment env = environment.acquire();
        if (env != null) env.applySettings(context);

        // camera
        Camera cam = camera.acquireOrThrow("Camera required.");
        context.getCamera().pushValue(cam, false);
        int w = cam.getWidth();
        int h = cam.getHeight();

        // color targets
        Texture2D[] colorTargets = new Texture2D[gbuffers.size()];
        int location = 0;
        for (DefinedAllocationSocket<TextureDef<Texture2D>, Texture2D> g : gbuffers) {
            g.getDef().setSize(w, h);
            colorTargets[location++] = g.acquire();
        }
        bufferDef.setColorTargets(colorTargets);

        // depth target
        depthDef.setSize(w, h);
        bufferDef.setDepthTarget(depth.acquire());

        // framebuffer
        FrameBuffer fbo = frameBuffer.acquire();
        fbo.setMultiTarget(true);
        context.getFrameBuffer().pushValue(fbo);
        context.clearBuffers();

        // parameters
        parameters.acquire(); // acquires to parameterMap

        // render
        for (GeometryQueue q : geometry.acquire()) {
            q.applySettings(context);
            q.render(context, this);
            q.restoreSettings(context);
        }

        // reset settings
        context.getCamera().pop();
        context.getFrameBuffer().pop();
        if (env != null) env.restoreSettings(context);

    }

    @Override
    public void renderGeometry(FrameGraphContext context, Geometry geometry) {
        Material mat = geometry.getMaterial();
        MaterialUtils.setParameters(mat, parameterMap, o -> o);
        context.getRenderManager().renderGeometry(geometry);
    }

    public int addBuffer(TextureDef<Texture2D> def) {
        gbuffers.add(new DefinedAllocationSocket<>(this, allocator, def));
        return gbuffers.size() - 1;
    }

    public ArgumentSocket<Object> getParameter(String name) {
        ArgumentSocket<Object> p = parameters.get(name);
        if (p == null) {
            p = new ArgumentSocket<>(this);
            parameters.put(name, p);
        }
        return p;
    }

    public CollectorSocket<GeometryQueue> getGeometry() {
        return geometry;
    }

    public TransitiveSocket<Camera> getCamera() {
        return camera;
    }

    public ArgumentSocket<RenderEnvironment> getEnvironment() {
        return environment;
    }

    public Socket<? extends List<Texture2D>> getGBuffers() {
        return gbuffers;
    }

    public Socket<Texture2D> getDepth() {
        return depth;
    }

    public TextureDef<Texture2D> getDepthDef() {
        return depthDef;
    }

}
