/*
 * Copyright (c) 2026 Clove Twilight
 * Licensed under the ESAL-1.3 Licence
 */

package win.doughmination.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Event fired when a player is unbanned via DoughAPI
 * Other plugins can listen to this event to perform actions when a player is unbanned.
 */
public class PlayerUnbannedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUUID;
    private final String playerName;
    private final String unbannedBy;

    public PlayerUnbannedEvent(UUID playerUUID, String playerName, String unbannedBy) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.unbannedBy = unbannedBy;
    }

    /**
     * Get the UUID of the unbanned player
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    /**
     * Get the username of the unbanned player
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * Get the name of who removed the ban
     */
    public String getUnbannedBy() {
        return unbannedBy;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}