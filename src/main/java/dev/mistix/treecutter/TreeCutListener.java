package dev.mistix.treecutter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class TreeCutListener implements Listener {
    private static final int[][] DIRECTIONS = createDirections();

    private final MistixTreeCutterPlugin plugin;
    private final Map<UUID, Long> cooldownUntilMillis = new ConcurrentHashMap<>();

    public TreeCutListener(MistixTreeCutterPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!"BREAK".equalsIgnoreCase(plugin.getConfig().getString("activation-mode", "BREAK"))) {
            return;
        }

        if (tryCutTree(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (!"SHIFT_LEFT_CLICK".equalsIgnoreCase(plugin.getConfig().getString("activation-mode", "BREAK"))) {
            return;
        }

        if (event.getAction() != Action.LEFT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        if (tryCutTree(event.getPlayer(), event.getClickedBlock())) {
            event.setCancelled(true);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    private boolean tryCutTree(Player player, Block start) {
        FileConfiguration config = plugin.getConfig();

        if (!plugin.getPlayerSettingsStore().isEnabled(player.getUniqueId())) {
            plugin.debug("Tree cutting skipped for " + player.getName() + ": player toggle disabled.");
            return false;
        }

        if (config.getBoolean("sneak-required", true) && !player.isSneaking()) {
            return false;
        }

        if (!player.hasPermission("mistix.treecutter.use")) {
            player.sendMessage(plugin.message("messages.no-permission", "&cYou don't have permission to use tree cutter."));
            return false;
        }

        if (!isWorldEnabled(start.getWorld().getName())) {
            plugin.debug("Tree cutting skipped in disabled world: " + start.getWorld().getName());
            return false;
        }

        if (!isLog(start.getType()) || !isAllowedLog(start.getType())) {
            return false;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isAllowedTool(tool)) {
            plugin.debug("Tree cutting skipped for " + player.getName() + ": tool not allowed.");
            return false;
        }

        if (config.getBoolean("require-bottom-log", true) && isLog(start.getRelative(0, -1, 0).getType())) {
            return false;
        }

        if (config.getBoolean("require-leaves", true) && !looksLikeNaturalTree(start)) {
            plugin.debug("Tree cutting skipped for " + player.getName() + ": no leaves detected.");
            return false;
        }

        if (!player.hasPermission("mistix.treecutter.bypasscooldown")) {
            long now = System.currentTimeMillis();
            long until = cooldownUntilMillis.getOrDefault(player.getUniqueId(), 0L);
            if (until > now) {
                long seconds = Math.max(1L, (until - now + 999L) / 1000L);
                player.sendMessage(plugin.message("messages.cooldown", "&cTree cutter cooldown: %seconds%s")
                        .replace("%seconds%", Long.toString(seconds)));
                return false;
            }
        }

        int maxLogs = Math.max(1, config.getInt("max-logs-per-operation", config.getInt("max-blocks-per-tree", 180)));
        int maxDistance = Math.max(1, config.getInt("max-distance-from-start-log", 12));
        Set<Block> logs = collectConnectedLogs(start, maxLogs, maxDistance);

        if (logs.size() <= 1) {
            return false;
        }

        if (!player.hasPermission("mistix.treecutter.bypasscooldown")) {
            int cooldownSeconds = resolveCooldownSeconds(player);
            cooldownUntilMillis.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
        }

        List<Block> orderedLogs = new ArrayList<Block>(logs);
        Collections.sort(orderedLogs, new java.util.Comparator<Block>() {
            @Override
            public int compare(Block left, Block right) {
                return Integer.compare(right.getY(), left.getY());
            }
        });

        Set<Block> leaves = config.getBoolean("break-leaves-with-tree", false)
                ? collectNearbyLeaves(logs, maxLogs * 3)
                : Collections.<Block>emptySet();

        for (Block log : orderedLogs) {
            breakBlock(log, tool);
        }

        for (Block leaf : leaves) {
            if (leaf.getType() != Material.AIR) {
                breakBlock(leaf, null);
            }
        }

        playCutSound(start.getLocation());
        plugin.debug("Tree cut by " + player.getName() + ": logs=" + logs.size() + ", leaves=" + leaves.size());
        return true;
    }

    private Set<Block> collectConnectedLogs(Block start, int limit, int maxDistance) {
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

            if (!isAllowedLog(current.getType())) {
                continue;
            }

            if (distanceFrom(start, current) > maxDistance) {
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

    private Set<Block> collectNearbyLeaves(Set<Block> logs, int limit) {
        Set<Block> leaves = new HashSet<Block>();
        for (Block log : logs) {
            for (int x = -2; x <= 2; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -2; z <= 2; z++) {
                        if (leaves.size() >= limit) {
                            return leaves;
                        }

                        Block neighbor = log.getRelative(x, y, z);
                        if (isLeaf(neighbor.getType())) {
                            leaves.add(neighbor);
                        }
                    }
                }
            }
        }
        return leaves;
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

    private boolean isWorldEnabled(String worldName) {
        List<String> enabledWorlds = plugin.getConfig().getStringList("enabled-worlds");
        List<String> disabledWorlds = plugin.getConfig().getStringList("disabled-worlds");

        if (containsIgnoreCase(disabledWorlds, worldName)) {
            return false;
        }

        return enabledWorlds.isEmpty() || containsIgnoreCase(enabledWorlds, worldName);
    }

    private boolean isAllowedLog(Material material) {
        List<String> allowed = plugin.getConfig().getStringList("allowed-log-materials");
        List<String> blocked = plugin.getConfig().getStringList("blocked-log-materials");

        if (containsIgnoreCase(blocked, material.name())) {
            return false;
        }

        return allowed.isEmpty() || containsIgnoreCase(allowed, material.name());
    }

    private boolean isAllowedTool(ItemStack item) {
        List<String> allowedTools = plugin.getConfig().getStringList("allowed-tools");
        if (allowedTools.isEmpty()) {
            return true;
        }

        String toolName = item == null || item.getType() == Material.AIR ? "HAND" : item.getType().name();
        return containsIgnoreCase(allowedTools, toolName);
    }

    private int resolveCooldownSeconds(Player player) {
        FileConfiguration config = plugin.getConfig();
        int cooldown = Math.max(0, config.getInt("cooldown-seconds.default", config.getInt("cooldown-seconds", 8)));
        ConfigurationSection tiers = config.getConfigurationSection("cooldown-seconds.permission-tiers");
        if (tiers == null) {
            return cooldown;
        }

        for (String permission : tiers.getKeys(false)) {
            if (player.hasPermission(permission)) {
                cooldown = Math.min(cooldown, Math.max(0, tiers.getInt(permission, cooldown)));
            }
        }
        return cooldown;
    }

    private void breakBlock(Block block, ItemStack tool) {
        if (block == null || block.getType() == Material.AIR) {
            return;
        }

        Material originalType = block.getType();
        World world = block.getWorld();
        Location location = block.getLocation().add(0.5D, 0.5D, 0.5D);
        boolean broken = tool == null || tool.getType() == Material.AIR ? block.breakNaturally() : block.breakNaturally(tool);
        if (broken) {
            spawnCutParticles(world, location);
            plugin.debug("Broke block " + originalType.name() + " at " + formatLocation(location));
        }
    }

    private void spawnCutParticles(World world, Location location) {
        if (!plugin.getConfig().getBoolean("effects.particles.enabled", true)) {
            return;
        }

        String particleName = plugin.getConfig().getString("effects.particles.type", "CLOUD");
        Particle particle;
        try {
            particle = Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid particle configured: " + particleName + ". Falling back to CLOUD.");
            particle = Particle.CLOUD;
        }

        int count = Math.max(1, plugin.getConfig().getInt("effects.particles.count", 12));
        double spread = Math.max(0.0D, plugin.getConfig().getDouble("effects.particles.spread", 0.25D));
        world.spawnParticle(particle, location, count, spread, spread, spread, 0.02D);
    }

    private void playCutSound(Location location) {
        if (!plugin.getConfig().getBoolean("effects.sound.enabled", true)) {
            return;
        }

        String soundName = plugin.getConfig().getString("effects.sound.type", "BLOCK_WOOD_BREAK");
        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Invalid sound configured: " + soundName + ". Falling back to BLOCK_WOOD_BREAK.");
            sound = Sound.BLOCK_WOOD_BREAK;
        }

        float volume = (float) plugin.getConfig().getDouble("effects.sound.volume", 0.8D);
        float pitch = (float) plugin.getConfig().getDouble("effects.sound.pitch", 1.0D);
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    private int distanceFrom(Block start, Block current) {
        return Math.max(
                Math.max(Math.abs(start.getX() - current.getX()), Math.abs(start.getY() - current.getY())),
                Math.abs(start.getZ() - current.getZ())
        );
    }

    private String formatLocation(Location location) {
        return location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private boolean containsIgnoreCase(List<String> values, String target) {
        for (String value : values) {
            if (value.equalsIgnoreCase(target)) {
                return true;
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

    private static int[][] createDirections() {
        List<int[]> directions = new ArrayList<int[]>();
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    directions.add(new int[] {x, y, z});
                }
            }
        }
        return directions.toArray(new int[directions.size()][]);
    }
}
