package org.cache;


import org.cache.core.LocalCache;
import org.cache.eviction.LruEvictionPolicy;

public class Main {
    public static void main(String[] args) {

        LruEvictionPolicy<String> lruEvictionPolicy = new LruEvictionPolicy<>();
        LocalCache<String, String> localCache = new LocalCache<>(2, lruEvictionPolicy);

        localCache.put("test", "first value", 100);

        System.out.println(localCache.get("test").orElse(null));

        localCache.put("test1", "second value", 100);
        System.out.println(localCache.get("test").orElse(null));
        localCache.put("test2", "third value", 100);


        System.out.println(localCache.size());
        System.out.println(localCache.get("test"));
        System.out.println(localCache.get("test1").orElse(null));


        System.out.println(localCache.metrics().getEvictions());
    }
}