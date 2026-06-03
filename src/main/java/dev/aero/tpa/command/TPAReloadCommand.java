package dev.aero.tpa.command;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// TPAReload command - reload the plugin configuration file
public class TPAReloadCommand implements CommandExecutor {

    private final AeroTPA plugin;

    public TPAReloadCommand(AeroTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");

        if (!sender.hasPermission("aerotpa.admin")) {
            if (sender instanceof Player player) {
                player.sendMessage(MessageUtil.formatMessage(prefix,
                        plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            } else {
                sender.sendMessage("No permission.");
            }
            return true;
        }

        plugin.reloadPluginConfig();

        String msg = plugin.getConfig().getString("messages.config-reloaded", "Configuration reloaded.");
        if (sender instanceof Player player) {
            player.sendMessage(MessageUtil.formatMessage(prefix, msg));
        } else {
            sender.sendMessage(MessageUtil.colorize(prefix + msg));
        }

        return true;
    }

    // Helper to colorize console output
    private String colorize(String msg) {
        return MessageUtil.colorize(msg);
    }
}
