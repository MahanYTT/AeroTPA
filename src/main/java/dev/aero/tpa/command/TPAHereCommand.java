package dev.aero.tpa.command;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.model.TPARequest;
import dev.aero.tpa.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

// TPAHere command - request a player to teleport to you
public class TPAHereCommand implements CommandExecutor {

    private final AeroTPA plugin;

    public TPAHereCommand(AeroTPA plugin) {
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

        if (!player.hasPermission("aerotpa.use.tpahere")) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(MessageUtil.formatMessage(prefix, "&cUsage: /tpahere <player>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.player-not-found", "Player not found."),
                    Map.of("player", args[0])));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.cannot-tpa-self", "&cCannot TPA to yourself.")));
            return true;
        }

        // If auto-confirm is enabled, skip the GUI
        if (plugin.getPlayerDataManager().isAutoConfirm(player.getUniqueId())) {
            plugin.getRequestManager().createRequest(player, target, TPARequest.Type.TPAHERE);
        } else {
            plugin.getGuiManager().openConfirmGUI(player, target, TPARequest.Type.TPAHERE);
        }

        return true;
    }
}
