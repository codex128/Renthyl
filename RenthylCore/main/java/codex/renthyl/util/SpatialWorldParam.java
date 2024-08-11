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
    protected String resultUserData;
    
    public SpatialWorldParam(T start, T inherit, String resultUserData) {
        this.start = start;
        this.inherit = inherit;
        this.resultUserData = resultUserData;
    }
    
    protected abstract T getLocalValue(Spatial spatial);
    
    public void apply(Spatial spatial) {
        T value = getLocalValue(spatial);
        if (spatial.getParent() != null) {
            T parentVal = spatial.getParent().getUserData(resultUserData);
            if (parentVal != null && isInherit(value)) {
                spatial.setUserData(resultUserData, parentVal);
                return;
            }
        }
        if (!isInherit(value)) {
            spatial.setUserData(resultUserData, value);
        } else {
            spatial.setUserData(resultUserData, start);
        }
    }
    public T getWorldValue(Spatial spatial) {
        return spatial.getUserData(resultUserData);
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
    public String getResultUserDataName() {
        return resultUserData;
    }
    
    public static SpatialWorldParam<String> renderQueueParam() {
        return new Queue();
    }
    public static SpatialWorldParam<RenderQueue.ShadowMode> shadowModeParam() {
        return new Shadow();
    }
    
    public static class Queue extends SpatialWorldParam<String> {
        
        public static final String USERDATA = "RenderQueue";
        public static final String RESULT = "ResultRenderQueue";
        
        public Queue() {
            super("Opaque", "Inherit", RESULT);
        }

        @Override
        protected String getLocalValue(Spatial spatial) {
            String value = spatial.getUserData(USERDATA);
            if (value == null) {
                value = spatial.getLocalQueueBucket().name();
            }
            return value;
        }
        
    }
    public static class Shadow extends SpatialWorldParam<RenderQueue.ShadowMode> {

        public static final String RESULT = "ResultShadowMode";
        
        public Shadow() {
            super(RenderQueue.ShadowMode.Off, RenderQueue.ShadowMode.Inherit, RESULT);
        }

        @Override
        protected RenderQueue.ShadowMode getLocalValue(Spatial spatial) {
            return spatial.getLocalShadowMode();
        }
        
    }
    
    
}
