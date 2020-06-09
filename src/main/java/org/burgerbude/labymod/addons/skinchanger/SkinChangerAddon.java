package org.burgerbude.labymod.addons.skinchanger;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.labymod.api.LabyModAddon;
import net.labymod.api.events.PluginMessageEvent;
import net.labymod.main.LabyMod;
import net.labymod.main.lang.LanguageManager;
import net.labymod.settings.elements.ControlElement;
import net.labymod.settings.elements.KeyElement;
import net.labymod.settings.elements.SettingsElement;
import net.labymod.utils.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.burgerbude.labymod.addons.skinchanger.management.entity.FakePlayer;
import org.burgerbude.labymod.addons.skinchanger.management.SkinManagement;
import org.burgerbude.labymod.addons.skinchanger.gui.GuiSkinChanger;
import org.burgerbude.labymod.addons.skinchanger.management.ConnectionHandler;
import org.burgerbude.labymod.addons.skinchanger.event.ServerSwitchEvent;
import org.burgerbude.labymod.addons.skinchanger.utility.ReflectionHelper;
import org.burgerbude.labymod.addons.skinchanger.utility.SkinType;
import org.lwjgl.input.Keyboard;

import java.util.*;

/**
 * The main class of the <b>SkinChanger</b> addon
 *
 * @author Robby
 */
public class SkinChangerAddon extends LabyModAddon {

    private ReflectionHelper reflectionHelper;
    private SkinManagement skinManagement;

    private ServerSwitchEvent serverSwitchEvent;

    private FakePlayer fakePlayer;
    private Map<MinecraftProfileTexture.Type, ResourceLocation> originalPlayerTextures;
    private SkinType originalSkinType;
    private NetworkPlayerInfo customNetworkPlayerInfo;

    private int key;

    @Override
    public void onEnable() {
        this.reflectionHelper = new ReflectionHelper();
        this.skinManagement = new SkinManagement(this);
        this.serverSwitchEvent = new ConnectionHandler(this, this.skinManagement);

        //Listens to the MC|Brand channel for the ServerSwitchEvent
        this.api.getEventManager().register((PluginMessageEvent) (channel, packetBuffer) -> {
            if (this.api.getCurrentServer() != null) {
                if (channel.equalsIgnoreCase("MC|Brand")) {
                    this.serverSwitchEvent.callSwitch();
                }
            } else {
                ServerData serverData = Minecraft.getMinecraft().getCurrentServerData();
                if (serverData == null && Minecraft.getMinecraft().isSingleplayer()) {
                    serverData = new ServerData("singleplayer", "localhost", false);
                    LabyMod.getInstance().onJoinServer(serverData);
                }
            }
        });

        this.api.registerForgeListener(this);
    }

    @Override
    public void loadConfig() {
        this.key = this.getConfig().has("key") ? this.getConfig().get("key").getAsInt() : Keyboard.KEY_RSHIFT;
    }

    @Override
    protected void fillSettings(List<SettingsElement> subSettings) {
        KeyElement skinChangerElement = new KeyElement("Open SkinChanger", this,
                new ControlElement.IconData(Material.THIN_GLASS), "key", this.key);

        skinChangerElement.addCallback(callback -> {
            this.key = callback;
            this.getConfig().addProperty("key", this.key);
            saveConfig();
        });

        subSettings.add(skinChangerElement);
    }

    /**
     * The event is technically called but the development team of LabyMod has a issue with the bytecode manipulation
     * (9.06.2020)
     *
     * @param event The key input event
     */
    @SubscribeEvent
    public void keyPressed(InputEvent.KeyInputEvent event) {
        if (this.key != -1 && Keyboard.isKeyDown(this.key)) {
            Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(new GuiSkinChanger(this, this.skinManagement)));
        }
    }

    /**
     * Translates the given key
     *
     * @param key      The key of the translation
     * @param fallback The fallback if the key doesn't translated
     * @return a translated key or the fallback
     */
    public String translate(String key, String fallback) {
        key = "skinchanger_" + key;
        String translate = LanguageManager.translate(key);
        return key.equals(translate) ? fallback : translate;
    }

    /**
     * Updates the {@link NetworkPlayerInfo} of a player
     *
     * @param player            The player for the new {@link NetworkPlayerInfo}
     * @param networkPlayerInfo The new {@link NetworkPlayerInfo}
     */
    public void updateNetworkPlayerInfo(EntityPlayerSP player, NetworkPlayerInfo networkPlayerInfo) {

        Class<?> superClass = player.getClass().getSuperclass();

        if (superClass == null) return;

        Arrays.stream(superClass.getDeclaredFields())
                .filter(declaredField -> declaredField.getType().equals(NetworkPlayerInfo.class))
                .forEach(declaredField ->
                        this.reflectionHelper.updateFieldValue(declaredField, player, networkPlayerInfo));
    }

    /**
     * Updates the original profile information
     *
     * @param playerTextures The textures of the player
     * @param type           The skin type of the player
     */
    public void updateOriginalProfile(Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures, SkinType type) {
        this.setOriginalPlayerTextures(playerTextures);
        this.setOriginalSkinType(type);
    }

    /**
     * Gets the original player textures
     *
     * @return original player textures
     */
    public Map<MinecraftProfileTexture.Type, ResourceLocation> originalPlayerTextures() {
        return this.originalPlayerTextures;
    }

    /**
     * Sets the original player textures
     *
     * @param originalPlayerTextures the textures to set
     */
    public void setOriginalPlayerTextures(Map<MinecraftProfileTexture.Type, ResourceLocation> originalPlayerTextures) {
        this.originalPlayerTextures = originalPlayerTextures;
    }

    /**
     * Gets the original skin type
     *
     * @return original skin type
     */
    public SkinType originalSkinType() {
        return this.originalSkinType;
    }

    /**
     * Sets the original skin type
     *
     * @param originalSkinType the type to set
     */
    public void setOriginalSkinType(SkinType originalSkinType) {
        this.originalSkinType = originalSkinType;
    }

    /**
     * Gets the custom {@link NetworkPlayerInfo}
     *
     * @return a custom {@link NetworkPlayerInfo}
     */
    public NetworkPlayerInfo customNetworkPlayerInfo() {
        return this.customNetworkPlayerInfo;
    }

    /**
     * Sets the custom {@link NetworkPlayerInfo}
     *
     * @param customNetworkPlayerInfo the {@link NetworkPlayerInfo} to set
     */
    public void setCustomNetworkPlayerInfo(NetworkPlayerInfo customNetworkPlayerInfo) {
        this.customNetworkPlayerInfo = customNetworkPlayerInfo;
    }

    /**
     * Gets the fake player
     *
     * @return a fake player
     */
    public FakePlayer fakePlayer() {
        return this.fakePlayer;
    }

    /**
     * Sets the fake player
     *
     * @param fakePlayer the fake to set
     */
    public void setFakePlayer(FakePlayer fakePlayer) {
        this.fakePlayer = fakePlayer;
    }

    public ReflectionHelper reflectionHelper() {
        return this.reflectionHelper;
    }
}
