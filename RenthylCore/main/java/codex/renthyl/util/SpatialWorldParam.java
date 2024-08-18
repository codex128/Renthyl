/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package codex.renthyl.util;

import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;

/**
 *
 * @author codex
 * @param <T>
 */
public abstract class SpatialWorldParam <T> {
    
    protected T start, inherit;
    
    public SpatialWorldParam(T start, T inherit) {
        this.start = start;
        this.inherit = inherit;
    }
    
    protected abstract T getLocalValue(Spatial spatial);
    protected abstract void saveWorldValue(Spatial spatial, T value);
    public abstract T getWorldValue(Spatial spatial);
    
    public void apply(Spatial spatial) {
        T value = getLocalValue(spatial);
        if (spatial.getParent() != null) {
            T parentVal = getWorldValue(spatial.getParent());
            if (parentVal != null && isInherit(value)) {
                saveWorldValue(spatial, parentVal);
                return;
            }
        }
        if (!isInherit(value)) {
            saveWorldValue(spatial, value);
        } else {
            saveWorldValue(spatial, start);
        }
    }
    
    private boolean isInherit(T value) {
        return value == null || value.equals(inherit);
    }
    
    public T getStart() {
        return start;
    }
    public T getInherit() {
        return inherit;
    }
    
    public static final SpatialWorldParam<String> RenderQueueParam = new SpatialWorldParam<>("Opaque", "Inherit") {
        
        private static final String USERDATA = "RenderQueue";
        private static final String RESULT = "ResultRenderQueue";

        @Override
        protected String getLocalValue(Spatial spatial) {
            String value = spatial.getUserData(USERDATA);
            if (value == null) {
                value = spatial.getLocalQueueBucket().name();
            }
            return value;
        }
        @Override
        protected void saveWorldValue(Spatial spatial, String value) {
            spatial.setUserData(RESULT, value);
        }
        @Override
        public String getWorldValue(Spatial spatial) {
            return spatial.getUserData(RESULT);
        }
        
    };
    public static final SpatialWorldParam<RenderQueue.ShadowMode> ShadowModeParam
            = new SpatialWorldParam<>(RenderQueue.ShadowMode.Off, RenderQueue.ShadowMode.Inherit) {

        public static final String RESULT = "ResultShadowMode";

        @Override
        protected RenderQueue.ShadowMode getLocalValue(Spatial spatial) {
            return spatial.getLocalShadowMode();
        }
        @Override
        protected void saveWorldValue(Spatial spatial, RenderQueue.ShadowMode value) {
            spatial.setUserData(RESULT, value.name());
        }
        @Override
        public RenderQueue.ShadowMode getWorldValue(Spatial spatial) {
            return Enum.valueOf(RenderQueue.ShadowMode.class, spatial.getUserData(RESULT));
        }
        
    };
    
    
}
