package org.Fluxoah.treeCutter.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.Fluxoah.treeCutter.TreeCutter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TimberPlaceholderExpansion extends PlaceholderExpansion {

    private final TreeCutter plugin;

    public TimberPlaceholderExpansion(TreeCutter plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "timber";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Fluxoah";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null || !params.equalsIgnoreCase("status")) {
            return "";
        }
        return plugin.getPermissionManager().isTreeCuttingEnabled(player)
                ? plugin.getMessageManager().format("placeholder-enabled")
                : plugin.getMessageManager().format("placeholder-disabled");
    }
}
