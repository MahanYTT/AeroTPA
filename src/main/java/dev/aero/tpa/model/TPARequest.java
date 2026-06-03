package dev.aero.tpa.model;

import java.util.UUID;

// Represents a TPA request
public class TPARequest {

    public enum Type {
        TPA,
        TPAHERE
    }

    private final UUID sender;
    private final UUID target;
    private final Type type;
    private final long createdAt;

    public TPARequest(UUID sender, UUID target, Type type) {
        this.sender = sender;
        this.target = target;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public Type getType() {
        return type;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    // Check if expired based on config timeout
    public boolean isExpired(int timeoutSeconds) {
        return (System.currentTimeMillis() - createdAt) > (timeoutSeconds * 1000L);
    }

    // Get a readable time ago string
    public String getTimeAgo() {
        long elapsed = (System.currentTimeMillis() - createdAt) / 1000;
        if (elapsed < 60) {
            return elapsed + "s";
        } else {
            return (elapsed / 60) + "m " + (elapsed % 60) + "s";
        }
    }

    // Get display name for the GUI
    public String getTypeDisplay() {
        return type == Type.TPA ? "TPA" : "TPAHere";
    }
}
