package org.burgerbude.labymod.addons.skinchanger;

import net.labymod.core.LabyModCore;
import net.labymod.core.asm.LabyModCoreMod;
import net.labymod.main.Source;

/**
 * @author Robby
 */
public class SkinChangerConstants {

    public static final boolean DEBUG = true;

    public static final boolean OLD_MC = Source.ABOUT_MC_VERSION.startsWith("1.8");
    public static final String SKIN_TYPE_FIELD = DEBUG ? "skinType" :
            LabyModCoreMod.isObfuscated() ? "f" : "skinType";
    public static final String PLAYER_TEXTURES_FIELD = DEBUG ? "playerTextures" :
            LabyModCoreMod.isObfuscated() ? "a" : "playerTextures";

}
