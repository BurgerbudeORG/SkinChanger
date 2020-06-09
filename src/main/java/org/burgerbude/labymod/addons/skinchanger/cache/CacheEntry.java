package org.burgerbude.labymod.addons.skinchanger.cache;

/**
 * Represents a cache entry
 *
 * @param <K> The type of identifier maintained by this entry
 * @author Robby
 */
public class CacheEntry<K> {

    private final K key;
    private final long request;

    /**
     * Default constructor
     *
     * @param key   The identifier of the entry
     * @param request The last request of the entry
     */
    public CacheEntry(K key, long request) {
        this.key = key;
        this.request = request;
    }

    /**
     * Gets the name of the entry
     *
     * @return the name of entry
     */
    public K key() {
        return this.key;
    }

    /**
     * Gets the last request of the etnry
     *
     * @return the last request
     */
    public long request() {
        return this.request;
    }
}
