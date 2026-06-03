package dev.aero.tpa.command;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// TPAToggle command - toggle incoming teleport requests on or off
public class TPAToggleCommand implements CommandExecutor {

    private final AeroTPA plugin;

    public TPAToggleCommand(AeroTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (!player.hasPermission("aerotpa.use.toggle")) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        boolean disabled = plugin.getPlayerDataManager().toggleRequests(player.getUniqueId());

        if (disabled) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.toggle-requests-off", "Incoming requests disabled.")));
        } else {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.toggle-requests-on", "Incoming requests enabled.")));
        }

        return true;
    }
}
