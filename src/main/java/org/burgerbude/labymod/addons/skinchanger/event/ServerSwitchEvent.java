package org.burgerbude.labymod.addons.skinchanger.event;

/**
 * Fired, when a player connected to another game server on the network
 *
 * @author Robby
 */
public interface ServerSwitchEvent {

    /**
     * Called, when a player connected to another game server on the network
     */
    void callSwitch();

}
