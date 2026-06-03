package dev.aero.tpa.manager;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.model.TPARequest;
import dev.aero.tpa.util.MessageUtil;
import dev.aero.tpa.util.TeleportTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Manages all active TPA requests and cooldowns
public class RequestManager {

    private final AeroTPA plugin;

    // Requests keyed by target player UUID
    private final Map<UUID, List<TPARequest>> pendingRequests = new ConcurrentHashMap<>();

    // Cooldown tracking for spam protection
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public RequestManager(AeroTPA plugin) {
        this.plugin = plugin;
    }

    // Creates a new request, returns false if blocked
    public boolean createRequest(Player sender, Player target, TPARequest.Type type) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        // Check cooldown
        if (isOnCooldown(sender.getUniqueId())) {
            long remaining = getCooldownRemaining(sender.getUniqueId());
            String msg = plugin.getConfig().getString("messages.cooldown-active", "&cPlease wait {seconds}s.");
            sender.sendMessage(MessageUtil.formatMessage(prefix, msg,
                    Map.of("seconds", String.valueOf(remaining))));
            return false;
        }

        // Check if target has disabled requests
        if (plugin.getPlayerDataManager().hasRequestsDisabled(target.getUniqueId())) {
            String msg = plugin.getConfig().getString("messages.requests-disabled", "{player} has requests disabled.");
            sender.sendMessage(MessageUtil.formatMessage(prefix, msg,
                    Map.of("player", target.getName())));
            return false;
        }

        // Prevent duplicate pending requests
        List<TPARequest> existing = pendingRequests.getOrDefault(target.getUniqueId(), new ArrayList<>());
        for (TPARequest req : existing) {
            if (req.getSender().equals(sender.getUniqueId()) && !req.isExpired(getExpireSeconds())) {
                String msg = plugin.getConfig().getString("messages.already-has-request", "&cYou already have a pending request.");
                sender.sendMessage(MessageUtil.formatMessage(prefix, msg,
                        Map.of("player", target.getName())));
                return false;
            }
        }

        // Create the request
        TPARequest request = new TPARequest(sender.getUniqueId(), target.getUniqueId(), type);
        pendingRequests.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(request);

        // Set cooldown
        cooldowns.put(sender.getUniqueId(), System.currentTimeMillis());

        // Notify sender
        String sentKey = type == TPARequest.Type.TPA ? "messages.request-sent" : "messages.request-here-sent";
        String sentMsg = plugin.getConfig().getString(sentKey, "Request sent to {player}.");
        sender.sendMessage(MessageUtil.formatMessage(prefix, sentMsg,
                Map.of("player", target.getName())));

        // Check auto-accept
        if (plugin.getPlayerDataManager().isAutoAccept(target.getUniqueId())) {
            acceptRequest(target, sender.getUniqueId());
            return true;
        }

        // Notify target
        String recvKey = type == TPARequest.Type.TPA ? "messages.request-received" : "messages.request-here-received";
        String recvMsg = plugin.getConfig().getString(recvKey, "{player} sent you a teleport request.");
        target.sendMessage(MessageUtil.formatMessage(prefix, recvMsg,
                Map.of("player", sender.getName())));

        // Schedule expiration task
        int expireSeconds = getExpireSeconds();
        Bukkit.getScheduler().runTaskLater(plugin, () -> expireRequest(request), expireSeconds * 20L);

        return true;
    }

    // Accept a specific request
    public boolean acceptRequest(Player target, UUID senderUUID) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        TPARequest request = findAndRemoveRequest(target.getUniqueId(), senderUUID);

        if (request == null) {
            Player senderPlayer = Bukkit.getPlayer(senderUUID);
            String name = senderPlayer != null ? senderPlayer.getName() : "Unknown";
            String msg = plugin.getConfig().getString("messages.no-request-from-player", "No request from {player}.");
            target.sendMessage(MessageUtil.formatMessage(prefix, msg,
                    Map.of("player", name)));
            return false;
        }

        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender == null || !sender.isOnline()) {
            String msg = plugin.getConfig().getString("messages.player-not-found", "Player is not online.");
            target.sendMessage(MessageUtil.formatMessage(prefix, msg,
                    Map.of("player", "Unknown")));
            return false;
        }

        // Notify both players
        String acceptedMsg = plugin.getConfig().getString("messages.request-accepted", "You accepted {player}'s request.");
        target.sendMessage(MessageUtil.formatMessage(prefix, acceptedMsg,
                Map.of("player", sender.getName())));

        String acceptedSenderMsg = plugin.getConfig().getString("messages.request-accepted-sender", "{player} accepted your request.");
        sender.sendMessage(MessageUtil.formatMessage(prefix, acceptedSenderMsg,
                Map.of("player", target.getName())));

        // Perform teleport
        if (request.getType() == TPARequest.Type.TPA) {
            // Sender teleports to target
            new TeleportTask(plugin, sender, target.getLocation()).start();
        } else {
            // Target teleports to sender
            new TeleportTask(plugin, target, sender.getLocation()).start();
        }

        return true;
    }

    // Accept the most recent request
    public boolean acceptLatestRequest(Player target) {
        List<TPARequest> requests = getRequestsFor(target.getUniqueId());
        if (requests.isEmpty()) {
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String msg = plugin.getConfig().getString("messages.no-pending-requests", "No pending requests.");
            target.sendMessage(MessageUtil.formatMessage(prefix, msg));
            return false;
        }

        // Get the newest request
        TPARequest latest = requests.get(requests.size() - 1);
        return acceptRequest(target, latest.getSender());
    }

    // Deny a specific request
    public boolean denyRequest(Player target, UUID senderUUID) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        TPARequest request = findAndRemoveRequest(target.getUniqueId(), senderUUID);

        if (request == null) {
            Player senderPlayer = Bukkit.getPlayer(senderUUID);
            String name = senderPlayer != null ? senderPlayer.getName() : "Unknown";
            String msg = plugin.getConfig().getString("messages.no-request-from-player", "No request from {player}.");
            target.sendMessage(MessageUtil.formatMessage(prefix, msg,
                    Map.of("player", name)));
            return false;
        }

        // Notify target
        Player sender = Bukkit.getPlayer(request.getSender());
        String senderName = sender != null ? sender.getName() : "Unknown";
        String deniedMsg = plugin.getConfig().getString("messages.request-denied", "You denied {player}'s request.");
        target.sendMessage(MessageUtil.formatMessage(prefix, deniedMsg,
                Map.of("player", senderName)));

        // Notify sender if online
        if (sender != null && sender.isOnline()) {
            String deniedSenderMsg = plugin.getConfig().getString("messages.request-denied-sender", "{player} denied your request.");
            sender.sendMessage(MessageUtil.formatMessage(prefix, deniedSenderMsg,
                    Map.of("player", target.getName())));
        }

        return true;
    }

    // Deny the most recent request
    public boolean denyLatestRequest(Player target) {
        List<TPARequest> requests = getRequestsFor(target.getUniqueId());
        if (requests.isEmpty()) {
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String msg = plugin.getConfig().getString("messages.no-pending-requests", "No pending requests.");
            target.sendMessage(MessageUtil.formatMessage(prefix, msg));
            return false;
        }

        TPARequest latest = requests.get(requests.size() - 1);
        return denyRequest(target, latest.getSender());
    }

    // Get all valid pending requests
    public List<TPARequest> getRequestsFor(UUID targetUUID) {
        List<TPARequest> requests = pendingRequests.getOrDefault(targetUUID, new ArrayList<>());
        int expireSeconds = getExpireSeconds();
        requests.removeIf(r -> r.isExpired(expireSeconds));
        return new ArrayList<>(requests);
    }

    // Clear all data
    public void clearAll() {
        pendingRequests.clear();
        cooldowns.clear();
    }

    // -- Private Helpers --

    private void expireRequest(TPARequest request) {
        List<TPARequest> requests = pendingRequests.get(request.getTarget());
        if (requests != null && requests.remove(request)) {
            String prefix = plugin.getConfig().getString("messages.prefix", "");

            // tell sender
            Player sender = Bukkit.getPlayer(request.getSender());
            Player target = Bukkit.getPlayer(request.getTarget());

            if (sender != null && sender.isOnline()) {
                String targetName = target != null ? target.getName() : "Unknown";
                String msg = plugin.getConfig().getString("messages.request-expired", "Your request to {player} expired.");
                sender.sendMessage(MessageUtil.formatMessage(prefix, msg,
                        Map.of("player", targetName)));
            }

            if (target != null && target.isOnline()) {
                String senderName = sender != null ? sender.getName() : "Unknown";
                String msg = plugin.getConfig().getString("messages.request-expired-target", "Request from {player} expired.");
                target.sendMessage(MessageUtil.formatMessage(prefix, msg,
                        Map.of("player", senderName)));
            }
        }
    }

    private TPARequest findAndRemoveRequest(UUID targetUUID, UUID senderUUID) {
        List<TPARequest> requests = pendingRequests.get(targetUUID);
        if (requests == null) return null;

        int expireSeconds = getExpireSeconds();
        Iterator<TPARequest> it = requests.iterator();
        while (it.hasNext()) {
            TPARequest req = it.next();
            if (req.getSender().equals(senderUUID) && !req.isExpired(expireSeconds)) {
                it.remove();
                return req;
            }
        }
        return null;
    }

    private boolean isOnCooldown(UUID uuid) {
        Long lastTime = cooldowns.get(uuid);
        if (lastTime == null) return false;
        int cooldownSeconds = plugin.getConfig().getInt("general.cooldown-seconds", 5);
        return (System.currentTimeMillis() - lastTime) < (cooldownSeconds * 1000L);
    }

    private long getCooldownRemaining(UUID uuid) {
        Long lastTime = cooldowns.get(uuid);
        if (lastTime == null) return 0;
        int cooldownSeconds = plugin.getConfig().getInt("general.cooldown-seconds", 5);
        long elapsed = (System.currentTimeMillis() - lastTime) / 1000;
        return Math.max(0, cooldownSeconds - elapsed);
    }

    private int getExpireSeconds() {
        return plugin.getConfig().getInt("general.request-expire-seconds", 60);
    }
}
