package dev.mistix.treecutter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class TreeCutListener implements Listener {
    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private final MistixTreeCutterPlugin plugin;
    private final Map<UUID, Long> cooldownUntilMillis = new ConcurrentHashMap<>();

    public TreeCutListener(MistixTreeCutterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block start = event.getBlock();

        if (!player.isSneaking()) {
            return;
        }

        if (!player.hasPermission("mistix.treecutter.use")) {
            player.sendMessage(message("messages.no-permission", "§cYou don't have permission to use tree cutter."));
            return;
        }

        if (!isLog(start.getType())) {
            return;
        }

        FileConfiguration config = plugin.getConfig();

        if (config.getBoolean("require-axe", true) && !isAxe(player.getInventory().getItemInMainHand())) {
            return;
        }

        if (config.getBoolean("require-bottom-log", true) && isLog(start.getRelative(0, -1, 0).getType())) {
            return;
        }

        if (!looksLikeNaturalTree(start)) {
            return;
        }

        if (!player.hasPermission("mistix.treecutter.bypasscooldown")) {
            long now = System.currentTimeMillis();
            long until = cooldownUntilMillis.getOrDefault(player.getUniqueId(), 0L);
            if (until > now) {
                long seconds = Math.max(1, (until - now + 999) / 1000);
                String message = message("messages.cooldown", "§cTree cutter cooldown: %seconds%s")
                        .replace("%seconds%", Long.toString(seconds));
                player.sendMessage(message);
                return;
            }

            int cooldownSeconds = Math.max(0, config.getInt("cooldown-seconds", 8));
            cooldownUntilMillis.put(player.getUniqueId(), now + cooldownSeconds * 1000L);
        }

        int maxBlocks = Math.max(1, config.getInt("max-blocks-per-tree", 180));
        Set<Block> logs = collectConnectedLogs(start, maxBlocks);

        if (logs.size() <= 1) {
            return;
        }

        event.setCancelled(true);
        ItemStack tool = player.getInventory().getItemInMainHand();
        for (Block log : logs) {
            if (log.getType() != Material.AIR) {
                log.breakNaturally(tool, true);
            }
        }
    }

    private Set<Block> collectConnectedLogs(Block start, int limit) {
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty() && visited.size() < limit) {
            Block current = queue.poll();
            if (current == null || visited.contains(current)) {
                continue;
            }

            if (!isLog(current.getType())) {
                continue;
            }

            visited.add(current);

            Location location = current.getLocation();
            for (int[] direction : DIRECTIONS) {
                Block neighbor = location.getWorld().getBlockAt(
                        location.getBlockX() + direction[0],
                        location.getBlockY() + direction[1],
                        location.getBlockZ() + direction[2]
                );
                if (!visited.contains(neighbor) && isLog(neighbor.getType())) {
                    queue.add(neighbor);
                }
            }
        }

        return visited;
    }

    private boolean looksLikeNaturalTree(Block bottomLog) {
        for (int y = 1; y <= 8; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    Material material = bottomLog.getRelative(x, y, z).getType();
                    if (isLeaf(material) || material == Material.NETHER_WART_BLOCK || material == Material.WARPED_WART_BLOCK) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isLog(Material material) {
        return Tag.LOGS.isTagged(material);
    }

    private static boolean isLeaf(Material material) {
        return Tag.LEAVES.isTagged(material);
    }

    private static boolean isAxe(ItemStack item) {
        if (item == null) {
            return false;
        }
        String name = item.getType().name();
        return name.endsWith("_AXE");
    }

    private String message(String path, String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }
}
