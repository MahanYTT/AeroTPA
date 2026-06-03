package dev.aero.tpa.util;

import dev.aero.tpa.AeroTPA;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

// Handles teleport warmup and movement cancellation
public class TeleportTask extends BukkitRunnable {

    private final AeroTPA plugin;
    private final Player player;
    private final Location destination;
    private final Location startLocation;
    private final int warmupSeconds;
    private final boolean cancelOnMove;
    private final double cancelDistance;
    private int countdown;

    public TeleportTask(AeroTPA plugin, Player player, Location destination) {
        this.plugin = plugin;
        this.player = player;
        this.destination = destination;
        this.startLocation = player.getLocation().clone();
        this.warmupSeconds = plugin.getConfig().getInt("general.warmup-seconds", 3);
        this.cancelOnMove = plugin.getConfig().getBoolean("general.cancel-on-move", true);
        this.cancelDistance = plugin.getConfig().getDouble("general.cancel-move-distance", 1.0);
        this.countdown = warmupSeconds;
    }

    @Override
    public void run() {
        // Check if player is still online
        if (!player.isOnline()) {
            cancel();
            return;
        }

        // Check for movement
        if (cancelOnMove && hasPlayerMoved()) {
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String msg = plugin.getConfig().getString("messages.teleport-cancelled-move", "&cTeleport cancelled! You moved.");
            player.sendMessage(MessageUtil.formatMessage(prefix, msg));
            cancel();
            return;
        }

        if (countdown <= 0) {
            // Perform teleport
            player.teleport(destination);
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String msg = plugin.getConfig().getString("messages.teleport-success", "&#7B68EE✦ &fTeleported successfully!");
            player.sendMessage(MessageUtil.formatMessage(prefix, msg));
            cancel();
            return;
        }

        // Send countdown message
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString("messages.teleporting-in", "Teleporting in {seconds} seconds...");
        Component message = MessageUtil.formatMessage(prefix, msg, Map.of("seconds", String.valueOf(countdown)));
        player.sendMessage(message);

        countdown--;
    }

    private boolean hasPlayerMoved() {
        Location current = player.getLocation();
        // Check horizontal and vertical distance, ignoring head rotation
        if (current.getWorld() != startLocation.getWorld()) return true;
        double dx = current.getX() - startLocation.getX();
        double dy = current.getY() - startLocation.getY();
        double dz = current.getZ() - startLocation.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) > cancelDistance;
    }

    // Start teleport warmup (instant if warmup is 0)
    public void start() {
        if (warmupSeconds <= 0) {
            player.teleport(destination);
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            String msg = plugin.getConfig().getString("messages.teleport-success", "&#7B68EE✦ &fTeleported successfully!");
            player.sendMessage(MessageUtil.formatMessage(prefix, msg));
        } else {
            this.runTaskTimer(plugin, 0L, 20L); // Run every second
        }
    }
}
