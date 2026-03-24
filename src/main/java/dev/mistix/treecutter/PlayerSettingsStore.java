package dev.mistix.treecutter;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class PlayerSettingsStore {
    private final MistixTreeCutterPlugin plugin;
    private final File file;
    private FileConfiguration configuration;

    public PlayerSettingsStore(MistixTreeCutterPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder.");
        }

        configuration = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isEnabled(UUID playerId) {
        return configuration.getBoolean("players." + playerId.toString() + ".enabled", true);
    }

    public boolean setEnabled(UUID playerId, boolean enabled) {
        configuration.set("players." + playerId.toString() + ".enabled", Boolean.valueOf(enabled));
        save();
        return enabled;
    }

    public boolean toggle(UUID playerId) {
        return setEnabled(playerId, !isEnabled(playerId));
    }

    private void save() {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players.yml", exception);
        }
    }
}