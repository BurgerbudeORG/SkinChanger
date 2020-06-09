package org.burgerbude.labymod.addons.skinchanger.utility;

import java.util.Arrays;

/**
 * An enumeration containing the available skin types
 *
 * @author Robby
 */
public enum SkinType {

    /**
     * The default skin with 4 pixel wide arms
     */
    STEVE("default"),
    /**
     * The alex skin with 3 pixel wide arms
     */
    ALEX("slim");

    private final String typeName;

    /**
     * Default constructor
     *
     * @param typeName The name of the skin type
     */
    SkinType(String typeName) {
        this.typeName = typeName;
    }

    /**
     * Gets the name of the skin type
     *
     * @return the type name
     */
    public String typeName() {
        return this.typeName;
    }

    /**
     * Gets the type of a skin with the given name
     *
     * @param name The name of the type
     * @return a {@link SkinType} or {@link #STEVE}
     */
    public static SkinType typeByName(String name) {
        return Arrays.stream(values()).filter(value -> value.typeName.equals(name)).findFirst().orElse(STEVE);
    }
}
