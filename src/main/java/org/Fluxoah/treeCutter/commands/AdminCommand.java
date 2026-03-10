package org.Fluxoah.treeCutter.commands;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.MessageManager;
import org.Fluxoah.treeCutter.managers.PermissionManager;
import org.Fluxoah.treeCutter.managers.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private final TreeCutter plugin;
    private final MessageManager msg;
    private final PermissionManager perm;
    private final ConfigManager config;
    private final StatsManager stats;

    public AdminCommand(TreeCutter plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageManager();
        this.perm = plugin.getPermissionManager();
        this.config = plugin.getConfigManager();
        this.stats = plugin.getStatsManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && !perm.canUseAdminCommand(player)) {
            player.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        if (!isKnownSubcommand(sub)) {
            sendAdminHelp(sender);
            return true;
        }

        if (sender instanceof Player player && !perm.canUseAdminSubcommand(player, sub)) {
            player.sendMessage(msg.getMessage("no-permission"));
            return true;
        }

        switch (sub) {
            case "on" -> handleServerToggle(sender, true);
            case "off" -> handleServerToggle(sender, false);
            case "reload" -> handleReload(sender);
            case "blacklist" -> handleBlacklist(sender, args);
            case "check" -> handleCheck(sender, args);
            default -> sendAdminHelp(sender);
        }
        return true;
    }

    private void handleServerToggle(CommandSender sender, boolean enabled) {
        if (enabled) {
            if (config.isServerEnabled()) {
                sender.sendMessage(msg.getMessage("plugin-already-on"));
                return;
            }
            config.setServerEnabled(true);
            sender.sendMessage(msg.getMessage("plugin-turned-on"));
            return;
        }

        if (!config.isServerEnabled()) {
            sender.sendMessage(msg.getMessage("plugin-already-off"));
            return;
        }
        config.setServerEnabled(false);
        sender.sendMessage(msg.getMessage("plugin-turned-off"));
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().loadConfig();
        plugin.getMessageManager().loadMessages();
        plugin.getPermissionManager().applyCommandPermissions();
        sender.sendMessage(msg.getMessage("config-reloaded"));
    }

    private void handleBlacklist(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg.getMessage("usage-tcadmin-blacklist"));
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.getMessage("usage-tcadmin-blacklist"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                if (!target.hasPlayedBefore() && !target.isOnline()) {
                    sender.sendMessage(msg.getMessage("player-not-found"));
                    return;
                }
                if (config.isBlacklisted(target.getUniqueId())) {
                    sender.sendMessage(msg.format("player-already-blacklisted", "player", readableName(target, args[2])));
                    return;
                }
                config.addToBlacklist(target.getUniqueId());
                if (target.isOnline()) {
                    Player online = target.getPlayer();
                    if (online != null) {
                        online.updateCommands();
                    }
                }
                sender.sendMessage(msg.format("player-blacklisted", "player", readableName(target, args[2])));
            }
            case "remove" -> {
                if (args.length < 3) {
                    sender.sendMessage(msg.getMessage("usage-tcadmin-blacklist"));
                    return;
                }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
                if (!config.isBlacklisted(target.getUniqueId())) {
                    sender.sendMessage(msg.format("player-not-blacklisted", "player", readableName(target, args[2])));
                    return;
                }
                config.removeFromBlacklist(target.getUniqueId());
                config.enableBlacklistMessage(target.getUniqueId());
                plugin.getNoticeManager().notifyUnblacklisted(target.getUniqueId());
                if (target.isOnline()) {
                    Player online = target.getPlayer();
                    if (online != null) {
                        online.updateCommands();
                    }
                }
                sender.sendMessage(msg.format("player-unblacklisted", "player", readableName(target, args[2])));
            }
            case "list" -> handleBlacklistList(sender);
            default -> sender.sendMessage(msg.getMessage("usage-tcadmin-blacklist"));
        }
    }

    private void handleBlacklistList(CommandSender sender) {
        if (config.getBlacklist().isEmpty()) {
            sender.sendMessage(msg.getMessage("blacklist-empty"));
            return;
        }

        sender.sendMessage(msg.getMessage("blacklist-header"));
        for (UUID uuid : config.getBlacklist()) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            sender.sendMessage(msg.format("blacklist-entry", "player", readableName(player, uuid.toString())));
        }
        sender.sendMessage(msg.getMessage("blacklist-footer"));
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(msg.getMessage("usage-tcadmin-check"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(msg.getMessage("player-not-found"));
            return;
        }
        UUID uuid = target.getUniqueId();
        String name = readableName(target, args[1]);
        if (!stats.hasStats(uuid)) {
            sender.sendMessage(msg.format("stats-no-data", "player", name));
            return;
        }

        sender.sendMessage(msg.format("stats-header", "player", name));
        var perType = stats.getPlayerStats(uuid);
        sender.sendMessage(msg.format("stats-total", "count", String.valueOf(stats.getTreesCut(uuid))));
        sender.sendMessage(msg.format("stats-favorite", "type", favoriteType(perType)));
        sender.sendMessage(msg.format("stats-first-cut", "date", formatDate(stats.getFirstCut(uuid))));
        sender.sendMessage(msg.format("stats-last-cut", "date", formatDate(stats.getLastCut(uuid))));
        sender.sendMessage(msg.getMessage("stats-separator"));
        int totalLogs = perType.values().stream().mapToInt(Integer::intValue).sum();
        perType.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(entry -> sender.sendMessage(msg.format(
                        "stats-wood-line",
                        "type", prettifyMaterial(entry.getKey()),
                        "count", String.valueOf(entry.getValue()),
                        "percent", formatPercent(entry.getValue(), totalLogs))));
        sender.sendMessage(msg.getMessage("stats-footer"));
    }

    private String readableName(OfflinePlayer player, String fallback) {
        return player.getName() != null ? player.getName() : fallback;
    }

    private void sendAdminHelp(CommandSender sender) {
        for (String line : msg.formatList("tcadmin-help")) {
            sender.sendMessage(line);
        }
    }

    private boolean isKnownSubcommand(String subCommand) {
        return switch (subCommand) {
            case "on", "off", "reload", "blacklist", "check", "help" -> true;
            default -> false;
        };
    }

    private String formatDate(long timestampMs) {
        if (timestampMs <= 0L) {
            return "Unknown";
        }
        java.time.Instant instant = java.time.Instant.ofEpochMilli(timestampMs);
        java.time.ZonedDateTime dateTime = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String favoriteType(java.util.Map<String, Integer> stats) {
        return stats.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(entry -> prettifyMaterial(entry.getKey()))
                .orElse("None");
    }

    private String prettifyMaterial(String materialName) {
        String[] parts = materialName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }
        return builder.toString().trim();
    }

    private String formatPercent(int count, int total) {
        if (total <= 0) {
            return "0";
        }
        double percent = (count * 100.0) / total;
        return String.format(Locale.US, "%.1f", percent);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player && !perm.canUseAdminCommand(player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return Stream.of("on", "off", "reload", "blacklist", "check", "help")
                    .filter(sub -> !(sender instanceof Player player) || perm.canUseAdminSubcommand(player, sub))
                    .filter(sub -> sub.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("blacklist")) {
            return Stream.of("add", "remove", "list")
                    .filter(sub -> sub.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("blacklist")
                && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("check")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }

        return Collections.emptyList();
    }
}
