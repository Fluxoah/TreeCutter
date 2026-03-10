package org.Fluxoah.treeCutter.managers;

import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Locale;

public class MessageManager {

    private final TreeCutter plugin;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix;

    public MessageManager(TreeCutter plugin) {
        this.plugin = plugin;
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        String configuredPrefix = plugin.getConfig().getString("prefix");
        if (configuredPrefix == null || configuredPrefix.isBlank()) {
            configuredPrefix = messages.getString("prefix", "&8[&3T&bC&8]");
        }
        prefix = colorize(configuredPrefix);
    }

    public String getMessage(String key) {
        String message = getRawMessage(key);
        if (message == null) {
            return ChatColor.RED + "Missing message: " + key;
        }
        return colorize(applyPrefix(message));
    }

    public String getMessageWithPrefix(String key) {
        return getMessageWithPrefix(key, new String[0]);
    }

    public String getMessageWithPrefix(String key, String... replacements) {
        String message = getRawMessage(key);
        if (message == null) {
            return ChatColor.RED + "Missing message: " + key;
        }
        String processed = colorize(applyReplacements(applyPrefix(message), replacements));
        if (message.contains("{prefix}")) {
            return processed;
        }
        return prefix + processed;
    }

    public String format(String key, String... replacements) {
        return applyReplacements(getMessage(key), replacements);
    }

    public java.util.List<String> formatList(String key, String... replacements) {
        java.util.List<String> lines = messages.getStringList(key);
        if (lines == null || lines.isEmpty()) {
            return java.util.List.of(format(key, replacements));
        }
        java.util.List<String> result = new java.util.ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(colorize(applyReplacements(applyPrefix(line), replacements)));
        }
        return result;
    }

    public String getPrefix() {
        return prefix;
    }

    private String applyReplacements(String message, String... replacements) {
        String result = message;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            if (!key.startsWith("{")) {
                key = "{" + key + "}";
            }
            result = result.replace(key, replacements[i + 1]);
        }
        return result;
    }

    private String applyPrefix(String message) {
        return message.replace("{prefix}", prefix);
    }

    private String getRawMessage(String key) {
        String message = messages.getString(key);
        if (message == null) {
            return null;
        }
        return switch (key) {
            case "blacklisted" -> normalizeBlacklistedMessage(message);
            case "blacklist-message-disabled" -> message.replace("/tvmsg off", "/tcmsg off");
            default -> message;
        };
    }

    private String normalizeBlacklistedMessage(String message) {
        String normalized = message.replace("/tvmsg off", "/tcmsg off").trim();

        String lowerKeyPrefix = "blacklisted:";
        if (normalized.toLowerCase(Locale.ROOT).startsWith(lowerKeyPrefix)) {
            normalized = normalized.substring(lowerKeyPrefix.length()).trim();
        }
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }

        if (!normalized.toLowerCase(Locale.ROOT).contains("/tcmsg off")) {
            normalized += " &8| &7Type &e/tcmsg off &7to hide this message";
        }
        return normalized;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
