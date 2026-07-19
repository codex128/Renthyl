package codex.renthyljme.shadowsnew;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.sockets.ArgumentSocket;
import codex.renthyl.sockets.Socket;
import codex.renthyl.sockets.allocation.DefinedAllocationSocket;
import codex.renthyl.tasks.AbstractTask;
import codex.renthyljme.shadow.ShadowMaskDef;
import com.jme3.asset.AssetManager;
import com.jme3.renderer.Camera;
import com.jme3.texture.Image;

public class ShadowMaskFactory extends AbstractTask {

    private final ArgumentSocket<Camera> camera = new ArgumentSocket<>(this);
    private final ArgumentSocket<Image.Format> format = new ArgumentSocket<>(this, Image.Format.R32UI);
    private final DefinedAllocationSocket<ShadowMaskDef, ShadowMask> mask;

    public ShadowMaskFactory(AssetManager assetManager, ResourceAllocator allocator) {
        addSockets(camera, format);
        mask = addSocket(new DefinedAllocationSocket<>(this, allocator, new ShadowMaskDef(assetManager)));
    }

    @Override
    protected void renderTask() {
        mask.getDef().setCamera(camera.acquireOrThrow());
        mask.getDef().getMapDef().setFormat(format.acquireOrThrow());
        mask.acquire();
    }

    public ArgumentSocket<Camera> getCamera() {
        return camera;
    }

    public ArgumentSocket<Image.Format> getFormat() {
        return format;
    }

    public Socket<ShadowMask> getMask() {
        return mask;
    }

}
