package org.Fluxoah.treeCutter.listeners;

import org.Fluxoah.treeCutter.TreeCutter;
import org.Fluxoah.treeCutter.hooks.WorldGuardHook;
import org.Fluxoah.treeCutter.managers.ConfigManager;
import org.Fluxoah.treeCutter.managers.MessageManager;
import org.Fluxoah.treeCutter.managers.NoticeManager;
import org.Fluxoah.treeCutter.managers.PermissionManager;
import org.Fluxoah.treeCutter.managers.PlacedLogManager;
import org.Fluxoah.treeCutter.managers.StatsManager;
import org.Fluxoah.treeCutter.utils.ActionBarUtil;
import org.Fluxoah.treeCutter.utils.TreeUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class TreeListener implements Listener {

    private final TreeCutter plugin;
    private final ConfigManager config;
    private final MessageManager msg;
    private final PermissionManager perm;
    private final StatsManager stats;
    private final NoticeManager notices;
    private final PlacedLogManager placedLogs;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, PendingConfirmation> confirmations = new ConcurrentHashMap<>();

    public TreeListener(TreeCutter plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.msg = plugin.getMessageManager();
        this.perm = plugin.getPermissionManager();
        this.stats = plugin.getStatsManager();
        this.notices = plugin.getNoticeManager();
        this.placedLogs = plugin.getPlacedLogManager();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        clearExpiredConfirmation(player);

        if (!config.getLogTypes().contains(block.getType())) {
            return;
        }

        if (!config.isServerEnabled()) {
            return;
        }

        if (!perm.canUseTreeFeller(player)) {
            return;
        }

        if (config.isPlayerDisabled(player.getUniqueId())) {
            return;
        }

        if (config.isSneakRequired() && !player.isSneaking()) {
            return;
        }

        if (config.isBlacklisted(player.getUniqueId())) {
            notices.showBlacklistWarning(player);
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (config.isRequireAxe() && !isAxe(tool)) {
            return;
        }

        double remainingCooldown = getCooldownRemaining(player);
        if (remainingCooldown > 0D) {
            player.sendMessage(msg.format("cooldown-active", "time", formatSeconds(remainingCooldown)));
            return;
        }

        WorldGuardHook worldGuardHook = plugin.getWorldGuardHook();
        if (worldGuardHook != null && worldGuardHook.isEnabled()
                && !worldGuardHook.canBreakAll(player, List.of(block))) {
            return;
        }

        TreeUtils.TreeScanResult scanResult = TreeUtils.scanTree(block, config);
        if (config.shouldBlockPlayerMadeTrees() && scanResult.naturalMismatch()) {
            return;
        }
        if (!scanResult.validTree() || scanResult.blocks().isEmpty()) {
            return;
        }

        List<Block> treeBlocks = TreeUtils.sortBottomToTop(scanResult.blocks());
        if (config.shouldBlockPlayerMadeTrees()
                && (config.isTrackPlayerPlacedLogs() || config.isTrackCommandPlacedLogs())
                && placedLogs.containsPlacedLog(treeBlocks)) {
            return;
        }

        if (worldGuardHook != null && worldGuardHook.isEnabled() && !worldGuardHook.canBreakAll(player, treeBlocks)) {
            if (config.isDenyPartialBreaks()) {
                return;
            }
            treeBlocks = treeBlocks.stream()
                    .filter(treeBlock -> worldGuardHook.canBreakAll(player, List.of(treeBlock)))
                    .toList();
            if (treeBlocks.isEmpty()) {
                return;
            }
        }

        boolean confirmedDurabilityBreak = false;
        int remainingDurability = getRemainingDurability(tool);
        if (shouldConfirm(treeBlocks.size(), remainingDurability)) {
            if (!consumeConfirmation(player, block)) {
                event.setCancelled(true);
                setPendingConfirmation(player, treeBlocks);
                ActionBarUtil.send(player, msg.format("tool-will-break",
                        "tool", toolName(tool),
                        "remaining", String.valueOf(remainingDurability),
                        "needed", String.valueOf(treeBlocks.size()),
                        "time", String.valueOf(config.getConfirmationTimeSeconds())));
                return;
            }
            confirmedDurabilityBreak = true;
        }

        event.setCancelled(true);

        int broken = chopTree(player, treeBlocks, tool, confirmedDurabilityBreak);
        if (broken > 0) {
            confirmations.remove(player.getUniqueId());
            setCooldown(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cooldowns.remove(uuid);
        confirmations.remove(uuid);
        notices.cancel(uuid);
    }

    private int chopTree(Player player, List<Block> treeBlocks, ItemStack tool, boolean suppressBreakMessage) {
        if (treeBlocks.isEmpty()) {
            return 0;
        }

        Map<String, Integer> logsCounted = new java.util.HashMap<>();
        for (Block block : treeBlocks) {
            Material type = block.getType();
            logsCounted.merge(type.name(), 1, Integer::sum);

            if (config.allowVanillaDrops()) {
                Collection<ItemStack> drops = block.getDrops(tool);
                for (ItemStack drop : drops) {
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), drop);
                }
            } else if (config.shouldDropItems()) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.35, 0.5), new ItemStack(type));
            }

            block.setType(Material.AIR, false);
        }

        stats.recordTreeCut(player.getUniqueId(), logsCounted);
        if (!config.shouldPreserveDurability()) {
            handleToolDurability(player, tool, treeBlocks.size(), suppressBreakMessage);
        }
        return treeBlocks.size();
    }

    private void handleToolDurability(Player player, ItemStack tool, int blocksBroken, boolean suppressBreakMessage) {
        if (tool == null || tool.getType().isAir()) {
            return;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable) || meta.isUnbreakable()) {
            return;
        }

        int durabilityLoss = applyUnbreaking(blocksBroken, tool.getEnchantmentLevel(Enchantment.UNBREAKING));
        int maxDurability = tool.getType().getMaxDurability();
        int newDamage = damageable.getDamage() + durabilityLoss;

        if (maxDurability <= 0) {
            return;
        }

        if (newDamage >= maxDurability) {
            player.getInventory().setItemInMainHand(null);
            if (!suppressBreakMessage) {
                player.sendMessage(msg.format("tool-broke",
                        "tool", toolName(tool),
                        "count", String.valueOf(blocksBroken)));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            return;
        }

        damageable.setDamage(newDamage);
        tool.setItemMeta(damageable);
    }

    private boolean isAxe(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) {
            return false;
        }
        return tool.getType().name().endsWith("_AXE");
    }

    private int getRemainingDurability(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) {
            return -1;
        }

        ItemMeta meta = tool.getItemMeta();
        if (!(meta instanceof Damageable damageable) || meta.isUnbreakable()) {
            return -1;
        }

        int maxDurability = tool.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return -1;
        }

        return Math.max(0, maxDurability - damageable.getDamage());
    }

    private boolean shouldConfirm(int treeSize, int remainingDurability) {
        return config.isConfirmationRequired()
                && !config.shouldPreserveDurability()
                && remainingDurability >= 0
                && treeSize >= remainingDurability;
    }

    private double getCooldownRemaining(Player player) {
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) {
            return 0D;
        }
        long cooldownMs = (long) (config.getCooldownSeconds() * 1000L);
        long remainingMs = cooldownMs - (System.currentTimeMillis() - lastUse);
        if (remainingMs <= 0L) {
            return 0D;
        }
        return remainingMs / 1000D;
    }

    private String formatSeconds(double seconds) {
        return String.valueOf((int) Math.ceil(seconds));
    }

    private void clearExpiredConfirmation(Player player) {
        PendingConfirmation pending = confirmations.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            confirmations.remove(player.getUniqueId());
        }
    }

    private boolean consumeConfirmation(Player player, Block block) {
        PendingConfirmation pending = confirmations.get(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (pending.expiresAt() < System.currentTimeMillis()) {
            confirmations.remove(player.getUniqueId());
            return false;
        }
        if (!pending.treeKeys().contains(blockKey(block))) {
            return false;
        }
        confirmations.remove(player.getUniqueId());
        return true;
    }

    private void setPendingConfirmation(Player player, List<Block> treeBlocks) {
        long expiresAt = System.currentTimeMillis() + (long) (config.getConfirmationTimeSeconds() * 1000L);
        confirmations.put(player.getUniqueId(), new PendingConfirmation(toBlockKeys(treeBlocks), expiresAt));
    }

    private Set<String> toBlockKeys(List<Block> blocks) {
        Set<String> keys = new HashSet<>(Math.max(16, blocks.size()));
        for (Block block : blocks) {
            keys.add(blockKey(block));
        }
        return keys;
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private int applyUnbreaking(int baseLoss, int unbreakingLevel) {
        if (unbreakingLevel <= 0) {
            return baseLoss;
        }

        int actualLoss = 0;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < baseLoss; i++) {
            if (random.nextInt(unbreakingLevel + 1) == 0) {
                actualLoss++;
            }
        }
        return actualLoss;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private String toolName(ItemStack tool) {
        if (tool == null || tool.getType().isAir()) {
            return "tool";
        }
        ItemMeta meta = tool.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        String raw = tool.getType().name().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder();
        for (String part : raw.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(' ');
        }
        return builder.toString().trim();
    }

    private record PendingConfirmation(Set<String> treeKeys, long expiresAt) {
    }
}
