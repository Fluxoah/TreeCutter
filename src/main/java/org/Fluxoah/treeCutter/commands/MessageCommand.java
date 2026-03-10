package org.Fluxoah.treeCutter.commands;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.MessageManager;
import org.Fluxoah.treeCutter.managers.PermissionManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Locale;
import java.util.List;

public class MessageCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final MessageManager msg;
    private final PermissionManager perm;

    public MessageCommand(TreeCutter plugin) {
        this.config = plugin.getConfigManager();
        this.msg = plugin.getMessageManager();
        this.perm = plugin.getPermissionManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.getMessage("player-only"));
            return true;
        }

        if (!perm.canUseMessageCommand(player)) {
            player.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        if (!config.isBlacklisted(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Unknown command. Type \"/help\" for help.");
            return true;
        }

        if (args.length > 1) {
            player.sendMessage(msg.getMessage("usage-tcmsg"));
            return true;
        }

        if (args.length == 1) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (!sub.equals("stop") && !sub.equals("off")) {
                player.sendMessage(msg.getMessage("usage-tcmsg"));
                return true;
            }
        }

        if (config.isBlacklistMessageDisabled(player.getUniqueId())) {
            player.sendMessage(msg.getMessage("blacklist-message-already-disabled"));
            return true;
        }

        config.disableBlacklistMessage(player.getUniqueId());
        player.sendMessage(msg.getMessage("blacklist-message-disabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}
