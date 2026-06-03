package dev.aero.tpa.manager;

import java.util.*;

// In-memory toggle states, wipes on restart
public class PlayerDataManager {

    // Players with auto-accept on
    private final Set<UUID> autoAccept = new HashSet<>();

    // Players who skip the GUI confirmation
    private final Set<UUID> autoConfirm = new HashSet<>();

    // Players with incoming requests disabled
    private final Set<UUID> requestsDisabled = new HashSet<>();

    // -- Auto Accept --

    public boolean isAutoAccept(UUID uuid) {
        return autoAccept.contains(uuid);
    }

    // Toggle auto-accept, returns current state
    public boolean toggleAutoAccept(UUID uuid) {
        if (autoAccept.contains(uuid)) {
            autoAccept.remove(uuid);
            return false;
        } else {
            autoAccept.add(uuid);
            return true;
        }
    }

    // -- Auto Confirm --

    public boolean isAutoConfirm(UUID uuid) {
        return autoConfirm.contains(uuid);
    }

    // Toggle auto-confirm, returns current state
    public boolean toggleAutoConfirm(UUID uuid) {
        if (autoConfirm.contains(uuid)) {
            autoConfirm.remove(uuid);
            return false;
        } else {
            autoConfirm.add(uuid);
            return true;
        }
    }

    // -- Request Toggle --

    public boolean hasRequestsDisabled(UUID uuid) {
        return requestsDisabled.contains(uuid);
    }

    // Toggle request receiving, returns current state
    public boolean toggleRequests(UUID uuid) {
        if (requestsDisabled.contains(uuid)) {
            requestsDisabled.remove(uuid);
            return false;
        } else {
            requestsDisabled.add(uuid);
            return true;
        }
    }

    // Cleanup data on quit
    public void clearPlayer(UUID uuid) {
        autoAccept.remove(uuid);
        autoConfirm.remove(uuid);
        requestsDisabled.remove(uuid);
    }
}
