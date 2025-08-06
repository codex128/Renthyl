package codex.renthyljme;

import codex.renthyl.resources.ResourceAllocator;
import codex.renthyl.tasks.utils.MapToList;
import codex.renthyljme.filter.FilterChain;
import codex.renthyljme.filter.PostProcessFilter;
import codex.renthyljme.geometry.GeometryQueue;
import codex.renthyljme.scene.ControlRenderPass;
import codex.renthyljme.scene.GeometryPass;
import codex.renthyljme.scene.OutputPass;
import codex.renthyljme.scene.SceneEnqueuePass;
import com.jme3.asset.AssetManager;

/**
 * Mimics the behavior of JMonkeyEngine's renderer.
 */
public class LegacyFrameGraph extends JmeFrameGraph {

    private final FilterChain filters = new FilterChain();

    public LegacyFrameGraph(AssetManager assetManager, ResourceAllocator allocator) {
        super(assetManager);
        add(new ControlRenderPass());
        SceneEnqueuePass queues = SceneEnqueuePass.withLegacyQueues();
        MapToList<String, GeometryQueue> queueOrder = new MapToList<>(new String[] {
            SceneEnqueuePass.OPAQUE,
            SceneEnqueuePass.SKY,
            SceneEnqueuePass.TRANSPARENT,
            SceneEnqueuePass.GUI,
            SceneEnqueuePass.TRANSLUCENT
        });
        GeometryPass geometry = new GeometryPass(allocator);
        OutputPass out = addTask(new OutputPass());
        queueOrder.getMap().setUpstream(queues.getQueues());
        geometry.getGeometry().addCollectionSource(queueOrder.getList());
        filters.getSceneColor().setUpstream(geometry.getOutColor());
        filters.getSceneDepth().setUpstream(geometry.getOutDepth());
        out.getColor().setUpstream(filters.getFilterResult());
        out.getDepth().setUpstream(geometry.getOutDepth());
    }

    public FilterChain getFilters() {
        return filters;
    }

    public <T extends PostProcessFilter> T addFilter(T filter) {
        return filters.add(filter);
    }

    public void removeFilter(PostProcessFilter filter) {
        filters.remove(filter);
    }

}
