package org.burgerbude.labymod.addons.skinchanger.gui;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.labymod.support.util.Debug;
import net.labymod.utils.Consumer;
import net.labymod.utils.UUIDFetcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.burgerbude.labymod.addons.skinchanger.SkinChangerAddon;
import org.burgerbude.labymod.addons.skinchanger.management.entity.FakePlayer;
import org.burgerbude.labymod.addons.skinchanger.management.SkinManagement;
import org.burgerbude.labymod.addons.skinchanger.utility.SkinType;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.*;

/**
 * Represents the <b>SkinChanger</b> gui
 *
 * @author Robby
 */
public class GuiSkinChanger extends GuiScreen {

    private final SkinManagement skinManagement;
    private final FakePlayer fakePlayer;
    private final SkinChangerAddon addon;

    //Rotating the fake player
    private boolean previewDragging;
    private boolean currentDragging;
    private boolean mouseOverPreview;

    private double dragPreviewX;
    private double dragPreviewY;
    private double mouseClickedX;
    private double mouseClickedY;
    private double clickedYaw;

    //Text field for the name
    private GuiTextField nameTextField;

    //Notification stuff
    private String notification;
    private long notificationRequest;
    private int fade;
    private boolean fadeOut;

    public GuiSkinChanger(SkinChangerAddon addon, SkinManagement skinManagement) {
        this.addon = addon;
        if (this.addon.fakePlayer() == null)
            this.addon.setFakePlayer(this.fakePlayer = new FakePlayer(Minecraft.getMinecraft().world.init()));
        else
            this.fakePlayer = this.addon.fakePlayer();
        this.skinManagement = skinManagement;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        //Adds the skin changer buttons
        this.buttonList.add(new GuiButton(0, 5, this.height / 2 - 108, 110, 20,
                this.addon.translate("button_preview_skin", "Preview Skin")));
        this.buttonList.add(new GuiButton(1, 5, this.height / 2 - 87, 110, 20,
                this.addon.translate("button_change_skin", "Change Skin")));
        this.buttonList.add(new GuiButton(2, 5, this.height / 2 - 66, 110, 20,
                this.addon.translate("button_back", "Back")));

        //Adds the uploader buttons
        this.buttonList.add(new GuiButton(4, 5, this.height / 2 - 30, 110, 20,
                this.addon.translate("button_upload_preview_skin", "Upload preview Skin")));
        this.buttonList.add(new GuiButton(3, 5, this.height / 2 - 9, 110, 20,
                this.addon.translate("button_upload_changed_skin", "Upload changed Skin")));

        //Adds the cache buttons
        this.buttonList.add(new GuiButton(5, 5, this.height / 2 + 27, 110, 20,
                this.addon.translate("button_open_skin_cache", "Open Skin Cache")));
        this.buttonList.add(new GuiButton(8, 5, this.height / 2 + 48, 110, 20,
                this.addon.translate("button_clear_skin_cache", "Clear Skin Cache")));

        Keyboard.enableRepeatEvents(true);
        //Adds a text field to typed the name of a player
        this.nameTextField = new GuiTextField(11, this.fontRenderer, this.width / 2 - 85, this.height / 2, 200, 20);
        this.nameTextField.setMaxStringLength(16);
        this.nameTextField.setFocused(true);

        //Adds utility buttons
        this.buttonList.add(new GuiButton(9, this.width - 102, this.height - 22, 100, 20,
                this.addon.translate("button_reset_skin", "Reset Skin")));


        //Updates the fake player
        this.updateFakePlayer();
        this.buttonList.add(new GuiButton(10, this.width - 102, 2, 100, 20,
                this.addon.translate("button_skin_type", "Type") + ": " +
                        this.skinManagement.skinType(this.fakePlayer.networkPlayerInfo())));

    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                if (this.textFieldIsEmpty()) return;
                this.userExists(callback -> this.mc.addScheduledTask(() -> {
                    this.skinManagement
                            .update(this.fakePlayer.networkPlayerInfo(), this.nameTextField.getText());
                    this.skinManagement
                            .updateSkinType(this.fakePlayer.networkPlayerInfo(), this.fakePlayer.skinType());
                    this.callNotification(NotificationType.INFO,
                            this.addon.translate("notification_preview_skin",
                                    "This is the player's skin!"));
                }));

                break;
            case 1:
                if (this.textFieldIsEmpty()) return;
                if (this.mc.getConnection() == null) return;

                this.userExists(callback -> {
                    if (callback) {
                        this.mc.addScheduledTask(() -> {

                            NetworkPlayerInfo playerInfo = this.mc.getConnection().getPlayerInfo(Minecraft.getMinecraft().player.getUniqueID());

                            this.skinManagement.update(playerInfo, this.nameTextField.getText());
                            this.skinManagement.updateSkinType(playerInfo, this.fakePlayer.skinType());
                            this.addon.updateNetworkPlayerInfo(this.mc.player, playerInfo);
                            this.addon.setCustomNetworkPlayerInfo(playerInfo);
                            this.updateFakePlayer();

                            this.callNotification(NotificationType.INFO,
                                    this.addon.translate("notification_change_skin",
                                            String.format("Now you have the skin of %s", "§e" + nameTextField.getText())));
                        });
                    }
                });

                break;
            case 2:
                this.mc.displayGuiScreen(null);
                break;
            case 5:
                if (!this.skinManagement.openSkinCache()) {
                    this.callNotification(NotificationType.ERROR, this.addon
                            .translate("skin_not_exists", "Skin cache doesn't exists!"));
                }
                break;
            case 8:
                this.skinManagement.cleanSkinCache();
                this.callNotification(NotificationType.INFO, this.addon.translate("clean_skin_cache",
                        "The cache was successfully cleared!"));
                break;
            case 9:
                this.skinManagement.reset();
                this.updateFakePlayer();
                this.callNotification(NotificationType.INFO, this.addon.translate("reset_skin",
                        "Your skin was reset!"));
                break;
            case 10:

                this.fakePlayer.setSkinType(this.fakePlayer.skinType() == SkinType.ALEX ?
                        SkinType.STEVE : SkinType.ALEX);
                button.displayString = this.addon.translate("button_skin_type", "Type") + ": " +
                        fakePlayer.skinType().name();
                this.skinManagement.updateSkinType(this.fakePlayer.networkPlayerInfo(),
                        fakePlayer.skinType());

                break;
        }
    }

    /**
     * Checks if the text field is empty
     *
     * @return true if the text field empty
     */
    private boolean textFieldIsEmpty() {
        if (this.nameTextField.getText().isEmpty()) {
            this.callNotification(NotificationType.ERROR,
                    this.addon.translate("notification_name_empty",
                            "Player without name cannot exists!"));
            return true;
        }
        return false;
    }

    /**
     * Checks if a player with that name exists!
     *
     * @param callback The callback returns <b>true</b> if a player exists with the name
     */
    private void userExists(Consumer<Boolean> callback) {
        UUIDFetcher.getUUID(this.nameTextField.getText(), uniqueId -> {
            if (uniqueId == null) {
                this.callNotification(NotificationType.WARN,
                        this.addon.translate("notification_player_not_exists",
                                "The wanted player doesn't exists!"));
                callback.accept(false);
                return;
            }

            callback.accept(true);
        });
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        this.nameTextField.updateCursorCounter();

        //Notification stuff START
        if (this.fadeOut) {
            if (this.fade <= 0) {
                this.fadeOut = false;
                this.fade = 0;
                return;
            }
            this.fade -= 5;
        }

        if (this.fade >= 255 && this.notificationRequest + 5000L <= System.currentTimeMillis()) {
            this.fadeOut = true;
        }
        //Notification stuff END
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Draws the screen
     *
     * @param mouseX       The x position of the mouse
     * @param mouseY       The y position of the mouse
     * @param partialTicks The partial ticks
     */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(0, 0, 120, height, 0x88000000);
        Gui.drawRect(120, 0, 121, height, -1);
        this.drawCenteredSideString("§6§lSkin §r§eChanger", this.height / 2 - 118, -1);
        this.drawCenteredSideString("§l" + this.addon.translate("title_upload", "Uploader"), this.height / 2 - 30 - 10, -1);
        this.drawCenteredSideString("§l" + this.addon.translate("title_cache", "Cache"), this.height / 2 + 17, -1);


        int positionX = this.width / 2 + 15;
        int positionY = this.height / 2 - 10;

        this.mouseOverPreview = mouseX >= positionX - 25 && mouseY >= positionY - 70 &&
                mouseX <= mouseX + 25 && mouseY <= positionY;

        this.drawEntityOnScreen(positionX, positionY, 40,
                this.width / 2 - mouseX, this.height / 2 - 10 - mouseY, this.fakePlayer);

        this.nameTextField.drawTextBox();

        String[] split = this.fakePlayer.getLocationSkin().getPath().split("/");

        String skinName = split[split.length - 1].replace(".png", "");
        skinName = skinName.substring(0, 1).toUpperCase() + skinName.substring(1).toLowerCase();

        this.fontRenderer.drawStringWithShadow("§7§l" + this.addon.translate("gui_current_skin", "Current skin")
                + "§8§l: §e" + skinName, 123, 2, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);

        this.fade = MathHelper.clamp(fade, 0, 255);
        int alpha;

        //Notification render
        if (this.fade > 8) {
            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

            alpha = this.fade << 24 & -16777216;

            fontRenderer.drawString(
                    notification,
                    this.width / 2 - this.fontRenderer.getStringWidth(notification) / 2 + (85 / 2) / 2,
                    this.height / 2 + 25,
                    16777215 | alpha,
                    true
            );
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }
    }

    /**
     * Fired when a key is typed
     *
     * @param typedChar The typed char
     * @param keyCode   The typed code
     * @throws IOException if a I/O error occurred
     */
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        this.nameTextField.textboxKeyTyped(typedChar, keyCode);
    }

    /**
     * Called when a mouse button is clicked
     *
     * @param mouseX      The x position of the mouse
     * @param mouseY      The y position of the mouse
     * @param mouseButton The clicked mouse button
     * @throws IOException if a I/O error occurred
     */
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.nameTextField.mouseClicked(mouseX, mouseY, mouseButton);

        if (!this.previewDragging && this.mouseOverPreview && mouseButton == 0) {
            this.previewDragging = true;
        }

        if (this.previewDragging && this.mouseOverPreview && mouseButton == 0) {
            this.currentDragging = true;

            this.mouseClickedX = (double) mouseX + this.dragPreviewX;
            this.mouseClickedY = (double) (this.clickedYaw > 180.0D ? -mouseY : mouseY) + this.dragPreviewY;
            this.clickedYaw = (this.dragPreviewX + 90.0D) % 360.0D;
        }

    }

    /**
     * Called when a mouse button is released
     *
     * @param mouseX The x position of the mouse
     * @param mouseY The y position of the mouse
     * @param state  The state of the mouse button
     */
    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            this.currentDragging = false;
            this.clickedYaw = (this.dragPreviewX + 90.0D) % 360.0D;
        }
    }

    /**
     * Called when a mouse button is pressed and the mouse is moved around
     *
     * @param mouseX             The x position of the mouse
     * @param mouseY             The y position of the mouse
     * @param clickedMouseButton The pressed button
     * @param timeSinceLastClick The time since last click
     */
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

        if (this.currentDragging) {
            this.dragPreviewX = ((double) (-mouseX) + this.mouseClickedX) % 360.0D;
            this.dragPreviewY = (double) (this.clickedYaw > 180.0D ? mouseY : -mouseY) + this.mouseClickedY;
            if (!Debug.isActive()) {
                if (this.dragPreviewY > 45.0D) {
                    this.dragPreviewY = 45.0D;
                }

                if (this.dragPreviewY < -45.0D) {
                    this.dragPreviewY = -45.0D;
                }
            }
        }

    }

    /**
     * Called when the screen is unloaded.
     */
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    /**
     * Draws a centered side string
     *
     * @param text  The string to be rendered
     * @param y     The y position of the string
     * @param color The string color
     */
    private void drawCenteredSideString(String text, int y, int color) {
        this.fontRenderer.drawStringWithShadow(text, 120 / 2 - this.fontRenderer.getStringWidth(text) / 2, y, color);
    }

    /**
     * Draws a player entity on the screen
     *
     * @param posX             The position x of the entity
     * @param posY             The position y of the entity
     * @param scale            The scale of the entity
     * @param mouseX           The mouse position x
     * @param mouseY           The mouse position y
     * @param entityLivingBase The living base of the entity
     */
    private void drawEntityOnScreen(int posX, int posY, int scale, float mouseX, float mouseY, EntityLivingBase entityLivingBase) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) posX, (float) posY, 50.0F);
        GlStateManager.scale((float) (-scale), (float) scale, (float) scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);

        float yawOffset = entityLivingBase.renderYawOffset;
        float rotYaw = entityLivingBase.rotationYaw;
        float rotPitch = entityLivingBase.rotationPitch;
        float preRotYawHead = entityLivingBase.prevRotationYawHead;
        float rotYawHead = entityLivingBase.rotationYawHead;
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate((float) -this.dragPreviewX, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((float) ((((float) Math.atan(mouseY / 40.0F)) * 20.0F) - this.dragPreviewY), 1.0F, 0.0F, 0.0F);
        entityLivingBase.renderYawOffset = (float) Math.atan(mouseX / 40.0F) * 20.0F;
        entityLivingBase.rotationYaw = (float) Math.atan(mouseX / 40.0F) * 40.0F;
        entityLivingBase.rotationPitch = -((float) Math.atan(mouseY / 40.0F)) * 20.0F;
        entityLivingBase.rotationYawHead = entityLivingBase.rotationYaw;
        entityLivingBase.prevRotationYawHead = entityLivingBase.rotationYaw;
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(0.0F);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntity(entityLivingBase, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        rendermanager.setRenderShadow(true);
        entityLivingBase.renderYawOffset = yawOffset;
        entityLivingBase.rotationYaw = rotYaw;
        entityLivingBase.rotationPitch = rotPitch;
        entityLivingBase.prevRotationYawHead = preRotYawHead;
        entityLivingBase.rotationYawHead = rotYawHead;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Updates the fake player profile
     */
    private void updateFakePlayer() {
        if (this.mc == null || this.mc.getConnection() == null) return;
        NetworkPlayerInfo networkPlayerInfo = this.addon.customNetworkPlayerInfo() == null ?
                this.mc.getConnection().getPlayerInfo(this.mc.player.getUniqueID()) :
                this.addon.customNetworkPlayerInfo();

        this.fakePlayer.setSkinType(SkinType.typeByName(networkPlayerInfo.getSkinType()));
        this.skinManagement.updateSkinType(fakePlayer.networkPlayerInfo(), SkinType.typeByName(networkPlayerInfo.getSkinType()));
        Map<MinecraftProfileTexture.Type, ResourceLocation> playerTextures = new HashMap<>();
        playerTextures.put(MinecraftProfileTexture.Type.SKIN, networkPlayerInfo.getLocationSkin());
        this.skinManagement.updatePlayerSkin(fakePlayer.networkPlayerInfo(), playerTextures);
    }

    /**
     * Calls a notification
     *
     * @param type    The type of the notification
     * @param message The message of the notification
     */
    private void callNotification(NotificationType type, String message) {
        String notificationType = type.color + this.addon.translate(type.key(), type.fallback());

        this.notification = notificationType + " §8§l| §7" + message;
        this.notificationRequest = System.currentTimeMillis();
        this.fade = 255;
        this.fadeOut = false;
    }

    /**
     * An enumeration containing the available notification types
     *
     * @author Robby
     */
    enum NotificationType {
        INFO("notification_info", "Info", "§a§l"),
        WARN("notification_warn", "Warning", "§c§l"),
        ERROR("notification_error", "Error", "§4§l");

        private final String key;
        private final String fallback;
        private final String color;

        NotificationType(String key, String fallback, String color) {
            this.key = key;
            this.fallback = fallback;
            this.color = color;
        }

        public String key() {
            return this.key;
        }

        public String fallback() {
            return this.fallback;
        }

        public String color() {
            return this.color;
        }
    }
}
