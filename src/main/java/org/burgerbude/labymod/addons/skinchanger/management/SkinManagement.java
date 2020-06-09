package org.burgerbude.labymod.addons.skinchanger.management;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.burgerbude.labymod.addons.skinchanger.SkinChangerAddon;
import org.burgerbude.labymod.addons.skinchanger.SkinChangerConstants;
import org.burgerbude.labymod.addons.skinchanger.cache.CacheConfiguration;
import org.burgerbude.labymod.addons.skinchanger.cache.CacheEntry;
import org.burgerbude.labymod.addons.skinchanger.utility.ReflectionHelper;
import org.burgerbude.labymod.addons.skinchanger.utility.SkinType;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * A management that manages all important things for skins
 *
 * @author Robby
 */
public class SkinManagement {

    private final Minecraft minecraft;
    private final ReflectionHelper reflectionHelper;
    private final SkinChangerAddon addon;
    private final CacheConfiguration<String> cacheConfiguration;

    private final File skinCacheDirectory;
    private final File skinChangerDirectory;

    /**
     * Default constructor
     *
     * @param addon The main addon
     */
    public SkinManagement(SkinChangerAddon addon) {
        this.addon = addon;
        this.reflectionHelper = this.addon.reflectionHelper();
        this.minecraft = Minecraft.getMinecraft();
        this.skinChangerDirectory = new File(Minecraft.getMinecraft().gameDir, "/SkinChanger/");
        this.skinCacheDirectory = new File(this.skinChangerDirectory, "/skins/");
        this.cacheConfiguration = new CacheConfiguration<>(new File(this.skinChangerDirectory, "skin_cache.json"));
    }

    /**
     * Updates the player textures of the {@link NetworkPlayerInfo} by the given name
     *
     * @param networkPlayerInfo The {@link NetworkPlayerInfo} to update the player textures
     * @param name              The username of the new player textures
     */
    public void update(NetworkPlayerInfo networkPlayerInfo, String name) {
        Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures = this.playerTextures(networkPlayerInfo);
        playerTextures.put(MinecraftProfileTexture.Type.SKIN, this.skin(name));
        this.updatePlayerSkin(networkPlayerInfo, playerTextures);
    }

    /**
     * Downloads or loads the skin with the given name
     *
     * @param name The name of a player
     * @return The location of the skin
     */
    public ResourceLocation skin(String name) {
        if (name != null && !name.isEmpty()) {
            name = name.toLowerCase();

            ResourceLocation resourceLocation = new ResourceLocation("skins/" + name);
            File skinFile = new File(this.skinCacheDirectory, name.toLowerCase() + ".png");

            CacheEntry<?> entry = this.cacheConfiguration.exists(name);
            if (entry != null) {
                if (entry.request() > 0L)
                    if (entry.request() + 1_800_000L <= System.currentTimeMillis()) {
                        skinFile.delete();
                        this.cacheConfiguration.removeCacheEntry(name);
                    }
            }


            //Downloads a skin from the URL or load the skin from file
            IImageBuffer imageBuffer = new ImageBufferDownload();
            ThreadDownloadImageData imageData = new ThreadDownloadImageData(
                    skinFile,
                    String.format("https://minotar.net/skin/%s", name),
                    DefaultPlayerSkin.getDefaultSkinLegacy(),
                    new IImageBuffer() {
                        @Override
                        public BufferedImage parseUserSkin(BufferedImage image) {
                            image = imageBuffer.parseUserSkin(image);
                            return image;
                        }

                        @Override
                        public void skinAvailable() {
                            imageBuffer.skinAvailable();
                        }
                    });

            //Adds the skin to cache configuration
            this.cacheConfiguration.add(new CacheEntry<>(name, System.currentTimeMillis()));
            try {
                Minecraft.getMinecraft().getTextureManager().loadTexture(resourceLocation, imageData);
            } catch (ReportedException e) {
                return this.networkPlayerInfo(this.minecraft.player).get().getLocationSkin();
            }
            return resourceLocation;
        }
        return this.networkPlayerInfo(this.minecraft.player).get().getLocationSkin();
    }

    /**
     * Updates the skin of the player with the {@link NetworkPlayerInfo}
     *
     * @param networkPlayerInfo The {@link NetworkPlayerInfo} of the player
     * @param playerTextures    The new textures
     */
    public void updatePlayerSkin(NetworkPlayerInfo networkPlayerInfo, Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures) {
        if (playerTextures == null) return;
        this.updatePlayerTextures(networkPlayerInfo, playerTextures);
    }

    /**
     * Gets the textures of the {@link NetworkPlayerInfo}
     *
     * @param networkPlayerInfo The player info to get
     * @return a {@link Map} which contains all player textures
     */
    public Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures(NetworkPlayerInfo networkPlayerInfo) {
        return Arrays.stream(networkPlayerInfo.getClass().getDeclaredFields())
                .filter(declaredField -> declaredField.getType().equals(Map.class) &&
                        declaredField.getName().equals(SkinChangerConstants.PLAYER_TEXTURES_FIELD))
                .findFirst()
                .map(declaredField -> (Map<MinecraftProfileTexture.Type, ResourceLocation>)
                        this.reflectionHelper.getFieldValue(declaredField, networkPlayerInfo))
                .orElse(new HashMap<>());
    }

    /**
     * Sets the new {@link SkinType} of the player
     *
     * @param object The player info to set the new {@link SkinType}
     * @param type   The type of the skin
     */
    public void updateSkinType(NetworkPlayerInfo object, SkinType type) {
        for (Field declaredField : object.getClass().getDeclaredFields()) {
            if (declaredField.getType().equals(String.class) && declaredField.getName().equals(SkinChangerConstants.SKIN_TYPE_FIELD)) {
                if (type == null) type = SkinType.STEVE;
                this.reflectionHelper.updateFieldValue(declaredField, object, type.typeName());
            }
        }
    }

    /**
     * Sets the a new {@link Field} value to the playerTextures
     *
     * @param networkPlayerInfo The network player info
     * @param playerTextures    The new player textures
     */
    public void updatePlayerTextures(NetworkPlayerInfo networkPlayerInfo, Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures) {
        for (Field declaredField : networkPlayerInfo.getClass().getDeclaredFields()) {
            if (declaredField.getType().equals(Map.class) && declaredField.getName().equals(SkinChangerConstants.PLAYER_TEXTURES_FIELD))
                this.reflectionHelper.updateFieldValue(declaredField, networkPlayerInfo, playerTextures);
        }
    }

    public SkinType skinType(NetworkPlayerInfo networkPlayerInfo) {
        return SkinType.typeByName(networkPlayerInfo.getSkinType());
    }

    /**
     * Resets the changed skin to the original
     */
    public void reset() {
        this.networkPlayerInfo(this.minecraft.player).ifPresent(networkPlayerInfo -> {
            this.updatePlayerTextures(networkPlayerInfo, this.addon.originalPlayerTextures());
            this.updateSkinType(networkPlayerInfo, this.addon.originalSkinType());
            this.addon.updateNetworkPlayerInfo(this.minecraft.player, networkPlayerInfo);
            this.addon.setCustomNetworkPlayerInfo(null);
        });
    }

    /**
     * Gets an optional {@link NetworkPlayerInfo}
     *
     * @param player The player to get the {@link NetworkPlayerInfo}
     * @return an optional player info
     */
    public Optional<NetworkPlayerInfo> networkPlayerInfo(EntityPlayerSP player) {
        return this.minecraft.getConnection() == null ?
                Optional.of(new NetworkPlayerInfo(player.getGameProfile())) :
                Optional.of(this.minecraft.getConnection().getPlayerInfo(player.getUniqueID()));

    }

    /**
     * Opens the skin directory it if exists
     *
     * @return <b>true</b> if the directory exists
     */
    public boolean openSkinCache() {
        if (!this.skinCacheDirectory.exists()) return false;
        OpenGlHelper.openFile(this.skinCacheDirectory);
        return true;
    }

    /**
     * Cleans the skin directory
     */
    public void cleanSkinCache() {
        this.cacheConfiguration.clearCache();
        this.cleanDirectory(this.skinChangerDirectory);
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param directory Directory to clean
     */
    private void cleanDirectory(File directory) {
        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
