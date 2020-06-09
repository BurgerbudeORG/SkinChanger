package org.burgerbude.labymod.addons.skinchanger.management;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.labymod.main.LabyMod;
import net.labymod.utils.ServerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.burgerbude.labymod.addons.skinchanger.SkinChangerAddon;
import org.burgerbude.labymod.addons.skinchanger.event.ServerSwitchEvent;
import org.burgerbude.labymod.addons.skinchanger.utility.SkinType;

import java.util.Map;
import java.util.UUID;

/**
 * Handles important connection stuff
 *
 * @author Robby
 */
public class ConnectionHandler implements ServerSwitchEvent {

    private final SkinChangerAddon addon;
    private final SkinManagement skinManagement;
    private final Minecraft minecraft;
    private boolean loadedProfile;

    /**
     * Default constructor
     *
     * @param addon          The main addon
     * @param skinManagement The management of the skins
     */
    public ConnectionHandler(SkinChangerAddon addon, SkinManagement skinManagement) {
        this.addon = addon;
        this.skinManagement = skinManagement;
        this.minecraft = Minecraft.getMinecraft();
        this.loadedProfile = false;
        this.addon.getApi().getEventManager().registerOnJoin(this::connect);
        this.addon.getApi().getEventManager().registerOnQuit(this::disconnect);
        this.addon.getApi().registerForgeListener(this);
    }

    /**
     * Updates the skin of a player when he connected to a server
     *
     * @param serverData Server information
     */
    public void connect(ServerData serverData) {
        this.minecraft.addScheduledTask(() -> {
            if (this.minecraft.getConnection() == null) return;

            if (this.addon.customNetworkPlayerInfo() != null) {
                this.addon.updateNetworkPlayerInfo(this.minecraft.player, this.addon.customNetworkPlayerInfo());
            }

        });
    }

    /**
     * Handles the disconnect logic
     *
     * @param serverData Server information
     */
    public void disconnect(ServerData serverData) {
        this.loadedProfile = false;
    }

    /**
     * Called when a player switching the backend server of a network
     */
    @Override
    public void callSwitch() {
        if (this.minecraft.getConnection() == null || this.addon.customNetworkPlayerInfo() == null) return;

        //Updates the network player info
        this.addon.updateNetworkPlayerInfo(this.minecraft.player, this.addon.customNetworkPlayerInfo());
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        //If the profile of the user loaded
        if (this.loadedProfile) return;
        if (event.phase != TickEvent.Phase.START || this.minecraft.player == null) return;

        //Gets the network player info
        NetworkPlayerInfo playerInfo = this.networkPlayerInfo(this.minecraft.player.getUniqueID());
        if (playerInfo == null) return;

        //Checks if the current server data not null
        if (LabyMod.getInstance().getCurrentServerData() != null) {

            Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures =
                    this.skinManagement.playerTextures(playerInfo);
            SkinType skinType = SkinType.typeByName(playerInfo.getSkinType());

            //If the original textures and original skin type null should update the
            if (this.addon.originalPlayerTextures() == null || this.addon.originalSkinType() == null) {
                this.addon.updateOriginalProfile(playerTextures, skinType);
            } else {
                //When the original textures not equals with the current textures should be update this
                if (!this.addon.originalPlayerTextures().equals(playerTextures))
                    this.addon.setOriginalPlayerTextures(playerTextures);

                //When the original skin type not equals with the current skin type should be update this
                if (!this.addon.originalSkinType().equals(skinType))
                    this.addon.setOriginalSkinType(skinType);

            }
            //Sets the network profile as loaded
            this.loadedProfile = true;
        }
    }

    /**
     * Gets the {@link NetworkPlayerInfo} by the given unique identifier
     *
     * @param uniqueId The unique identifier
     * @return the {@link NetworkPlayerInfo} with the unique identifier or <b>null</b>
     */
    private NetworkPlayerInfo networkPlayerInfo(UUID uniqueId) {
        return this.minecraft.getConnection() == null ? null : this.minecraft.getConnection().getPlayerInfo(uniqueId);

    }

}
