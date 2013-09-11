import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

/**
 *
 */
public class CacheManager {
    private static CacheManager instance;
    public Cache<String, Object> cache;

    protected CacheManager() {
        //
    }

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
            instance.cache = CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(1, TimeUnit.MINUTES)
                    .build();
        }
        return instance;
    }
}
