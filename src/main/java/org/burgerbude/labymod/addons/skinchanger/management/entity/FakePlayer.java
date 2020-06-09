package org.burgerbude.labymod.addons.skinchanger.management.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.burgerbude.labymod.addons.skinchanger.utility.SkinType;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * An object that representing a fake player
 *
 * @author Robby
 */
public class FakePlayer extends AbstractClientPlayer {

    private static final GameProfile FAKE_GAME_PROFILE = new GameProfile(
            UUID.nameUUIDFromBytes("LabyModFakePlayer".getBytes()),
            "LabyMod SkinChanger"
    );
    private NetworkPlayerInfo networkPlayerInfo;
    private SkinType skinType;

    public FakePlayer(World worldIn) {
        super(worldIn, FAKE_GAME_PROFILE);
        this.skinType = SkinType.typeByName(this.getSkinType());
    }

    @Nullable
    @Override
    protected NetworkPlayerInfo getPlayerInfo() {
        return this.networkPlayerInfo == null ?
                this.networkPlayerInfo = new NetworkPlayerInfo(FAKE_GAME_PROFILE) :
                this.networkPlayerInfo;
    }

    public NetworkPlayerInfo networkPlayerInfo() {
        return this.getPlayerInfo();
    }

    public SkinType skinType() {
        return this.skinType;
    }

    public void setSkinType(SkinType skinType) {
        this.skinType = skinType;
    }

    @Override
    public Vec3d getPositionVector() {
        return new Vec3d(0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean isWearing(EnumPlayerModelParts part) {
        return true;
    }

    @Override
    public boolean hasPlayerInfo() {
        return this.networkPlayerInfo != null;
    }

    @Override
    public boolean canUseCommand(int permLevel, String commandName) {
        return false;
    }

    @Override
    public boolean isEntityInvulnerable(DamageSource source) {
        return true;
    }

    @Override
    public boolean canAttackPlayer(EntityPlayer other) {
        return false;
    }
}
