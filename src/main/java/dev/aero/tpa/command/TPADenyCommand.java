package dev.aero.tpa.command;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// TPADeny command - deny a teleport request
public class TPADenyCommand implements CommandExecutor {

    private final AeroTPA plugin;

    public TPADenyCommand(AeroTPA plugin) {
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

        if (!player.hasPermission("aerotpa.use.deny")) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        if (args.length >= 1) {
            Player senderPlayer = Bukkit.getPlayerExact(args[0]);
            if (senderPlayer == null) {
                player.sendMessage(MessageUtil.formatMessage(prefix,
                        plugin.getConfig().getString("messages.player-not-found", "Player not found."),
                        java.util.Map.of("player", args[0])));
                return true;
            }
            plugin.getRequestManager().denyRequest(player, senderPlayer.getUniqueId());
        } else {
            plugin.getRequestManager().denyLatestRequest(player);
        }

        return true;
    }
}
