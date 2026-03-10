package org.Fluxoah.treeCutter.commands;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.MessageManager;
import org.Fluxoah.treeCutter.managers.NoticeManager;
import org.Fluxoah.treeCutter.managers.PermissionManager;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class TreeCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager config;
    private final MessageManager msg;
    private final PermissionManager perm;
    private final NoticeManager notices;

    public TreeCommand(TreeCutter plugin) {
        this.config = plugin.getConfigManager();
        this.msg = plugin.getMessageManager();
        this.perm = plugin.getPermissionManager();
        this.notices = plugin.getNoticeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(msg.getMessage("player-only"));
            return true;
        }

        if (!perm.canUseCommand(player)) {
            player.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        String subCommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (!isKnownSubcommand(subCommand)) {
            sendUsage(player);
            return true;
        }

        if (!perm.canUsePlayerSubcommand(player, subCommand)) {
            player.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        switch (subCommand) {
            case "on" -> enable(player);
            case "off" -> disable(player);
            case "status" -> sendStatus(player);
            case "help" -> sendHelp(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void enable(Player player) {
        if (config.isBlacklisted(player.getUniqueId())) {
            notices.showBlacklistWarning(player);
            return;
        }

        if (!config.isPlayerDisabled(player.getUniqueId())) {
            player.sendMessage(msg.getMessage("tc-already-enabled"));
            return;
        }

        config.removeDisabledPlayer(player.getUniqueId());
        player.sendMessage(msg.getMessage("tc-enabled"));
        playToggleSound(player);
    }

    private void disable(Player player) {
        if (config.isBlacklisted(player.getUniqueId())) {
            notices.showBlacklistWarning(player);
            return;
        }

        if (config.isPlayerDisabled(player.getUniqueId())) {
            player.sendMessage(msg.getMessage("tc-already-disabled"));
            return;
        }

        config.addDisabledPlayer(player.getUniqueId());
        player.sendMessage(msg.getMessage("tc-disabled"));
        playToggleSound(player);
    }

    private void sendStatus(Player player) {
        if (!config.isServerEnabled()) {
            player.sendMessage(msg.getMessage("plugin-disabled"));
            return;
        }
        if (config.isBlacklisted(player.getUniqueId())) {
            player.sendMessage(msg.getMessage("blacklisted"));
            return;
        }
        if (!perm.canUseTreeFeller(player)) {
            player.sendMessage(msg.getMessage("no-permission"));
            return;
        }
        if (config.isPlayerDisabled(player.getUniqueId())) {
            player.sendMessage(msg.getMessage("player-disabled"));
            return;
        }
        player.sendMessage(msg.getMessage("tc-enabled"));
    }

    private void sendHelp(Player player) {
        for (String line : msg.formatList("tc-help")) {
            player.sendMessage(line);
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(msg.getMessage("usage-tc"));
    }

    private void playToggleSound(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private boolean isKnownSubcommand(String subCommand) {
        return switch (subCommand) {
            case "on", "off", "status", "help" -> true;
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || !perm.canUseCommand(player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("on", "off", "status", "help")
                    .filter(sub -> perm.canUsePlayerSubcommand(player, sub))
                    .filter(sub -> sub.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        return Collections.emptyList();
    }
}
