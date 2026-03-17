package dev.mistix.treecutter;

import org.bukkit.plugin.java.JavaPlugin;

public class MistixTreeCutterPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new TreeCutListener(this), this);
        getLogger().info("MistixTreeCutter enabled.");
    }
}
