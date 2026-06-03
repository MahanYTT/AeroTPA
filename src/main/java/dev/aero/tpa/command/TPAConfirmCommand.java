package dev.aero.tpa.command;

import dev.aero.tpa.AeroTPA;
import dev.aero.tpa.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// TPAConfirm command - toggle automatic GUI confirmation
public class TPAConfirmCommand implements CommandExecutor {

    private final AeroTPA plugin;

    public TPAConfirmCommand(AeroTPA plugin) {
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

        if (!player.hasPermission("aerotpa.use.autoconfirm")) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        boolean enabled = plugin.getPlayerDataManager().toggleAutoConfirm(player.getUniqueId());

        if (enabled) {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.auto-confirm-enabled", "Auto-confirm enabled.")));
        } else {
            player.sendMessage(MessageUtil.formatMessage(prefix,
                    plugin.getConfig().getString("messages.auto-confirm-disabled", "Auto-confirm disabled.")));
        }

        return true;
    }
}
