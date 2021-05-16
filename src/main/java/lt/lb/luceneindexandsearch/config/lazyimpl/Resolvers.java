package lt.lb.luceneindexandsearch.config.lazyimpl;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import lt.lb.commons.containers.caching.lazy.LazyValue;
import lt.lb.luceneindexandsearch.config.Resolver;
import lt.lb.uncheckedutils.func.UncheckedSupplier;

/**
 *
 * @author laim0nas100
 */
public abstract class Resolvers {

    public static <P, V> Resolver<P, V> constant(V val) {
        return p -> val;
    }

    public static <P, V> Resolver<P, V> lazy(UncheckedSupplier<V> supl) {
        LazyValue<V> lazy = new LazyValue<>(supl);

        return p -> lazy.get();
    }

    public static <P, K, V> Resolver<P, V> keyCachedFactory(Resolver<P, K> func, Resolver<K, V> factory) {
        ConcurrentHashMap<WeakReference<K>, V> map = new ConcurrentHashMap<>();

        return p -> {
            K key = func.apply(p);
            WeakReference<K> weakRef = new WeakReference<>(key);
            return map.computeIfAbsent(weakRef, k -> factory.apply(key));
        };
    }

}
