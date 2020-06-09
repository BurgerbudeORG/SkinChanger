package org.burgerbude.labymod.addons.skinchanger.cache;

import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Represents a cache configuration
 *
 * @param <T> Type of the entry identifier
 * @author Robby
 */
public class CacheConfiguration<T> {

    private final File cacheFile;
    private final Gson gson;
    private final JsonParser jsonParser;

    private JsonObject cacheConfiguration;
    private JsonArray skinArray;

    /**
     * Default constructor
     *
     * @param cacheFile The cache configuration file
     */
    public CacheConfiguration(File cacheFile) {
        this.cacheFile = cacheFile;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.jsonParser = new JsonParser();
        this.cacheConfiguration = new JsonObject();
        this.skinArray = new JsonArray();
        this.load();
    }

    /**
     * Saves the cache configuration
     */
    public void save() {
        try {
            if (!this.cacheFile.exists()) {
                this.cacheFile.createNewFile();
            }

            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(this.cacheFile), StandardCharsets.UTF_8));
            printWriter.println(this.gson.toJson(this.cacheConfiguration));
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the cache configuration
     */
    public void load() {
        try {
            if(!this.cacheFile.exists()) return;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(this.cacheFile)));

            String line = bufferedReader.readLine();
            StringBuilder stringBuilder = new StringBuilder();
            while (line != null) {
                stringBuilder.append(line);
                line = bufferedReader.readLine();
            }

            if(stringBuilder.toString().isEmpty()) return;
            this.cacheConfiguration = this.jsonParser.parse(stringBuilder.toString()).getAsJsonObject();

            if (this.cacheConfiguration.has("skins") && this.cacheConfiguration.get("skins").isJsonArray()) {
                this.skinArray = this.cacheConfiguration.get("skins").getAsJsonArray();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a {@link CacheEntry} to the cache configuration and save it
     *
     * @param entry The entry to be added
     */
    public void add(CacheEntry<T> entry) {
        if (exists(entry.key()) == null) {
            this.cacheConfiguration.add("skins", this.addArray(entry));
            this.save();
        }

    }

    /**
     * Adds a {@link CacheEntry} to the skins array
     *
     * @param entry The entry to be added
     * @return the current skin cache array
     */
    public JsonArray addArray(CacheEntry<T> entry) {
        this.skinArray.add(this.jsonParser.parse(gson.toJson(entry)));
        return this.skinArray;
    }

    /**
     * Gets a {@link CacheEntry} from the cache with the given type
     *
     * @param type The type of entry
     * @return a entry or <b>null</b>
     */
    public CacheEntry<T> exists(T type) {
        if (this.isEmpty()) return null;
        for (JsonElement element : this.skinArray) {
            CacheEntry<T> entry = this.cacheEntryFromJson(element);

            if (entry.key().equals(type)) return entry;
        }
        return null;
    }

    /**
     * Gets from a {@link CacheEntry} with the given type the last request.
     *
     * @param type The type of entry
     * @return the last request from the entry or <b>0L</b>, when the entry doesn't exists
     */
    public long requestByType(T type) {
        if (this.isEmpty()) return 0L;

        CacheEntry<T> cacheEntry = this.exists(type);
        if (cacheEntry == null) return 0L;

        return cacheEntry.request();
    }

    /**
     * Removes a {@link CacheEntry} from the cache if is exists
     *
     * @param type The type of entry
     */
    public void removeCacheEntry(T type) {
        if (this.isEmpty()) return;

        CacheEntry<T> entry = this.exists(type);
        if (entry == null) return;

        JsonElement element = this.jsonParser.parse(this.gson.toJson(entry));
        this.skinArray.remove(element);
    }

    /**
     * Checks if the {@link #skinArray} <b>null</b> or empty
     *
     * @return true if the array <b>null</b> or empty
     */
    private boolean isEmpty() {
        return this.skinArray == null || this.skinArray.size() == 0;
    }

    /**
     * Deserializes the Json to a {@link CacheEntry}
     *
     * @param element The Json
     * @return <b>null</b> if <b>json</b> is <b>null</b> or the deserialized {@link CacheEntry}
     */
    private CacheEntry<T> cacheEntryFromJson(JsonElement element) {
        return this.gson.fromJson(element, CacheEntry.class);
    }


    public void clearCache() {
        while (skinArray.size() > 0) skinArray.remove(0);
    }
}
