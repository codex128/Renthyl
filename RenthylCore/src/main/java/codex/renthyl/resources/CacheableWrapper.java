package codex.renthyl.resources;

public interface CacheableWrapper<T> extends ResourceWrapper<T> {

    void cache(boolean cache);

}
