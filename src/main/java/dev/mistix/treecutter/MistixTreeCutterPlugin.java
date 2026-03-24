package dev.mistix.treecutter;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class MistixTreeCutterPlugin extends JavaPlugin {
    private PlayerSettingsStore playerSettingsStore;
    private boolean debugEnabled;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        playerSettingsStore = new PlayerSettingsStore(this);
        playerSettingsStore.load();
        reloadPluginState();

        TreeCutListener listener = new TreeCutListener(this);
        getServer().getPluginManager().registerEvents(listener, this);

        TreeCutterCommand commandHandler = new TreeCutterCommand(this);
        registerCommand("treecutter", commandHandler);
        registerCommand("treecutterhelp", commandHandler);

        getLogger().info("MistixTreeCutter enabled.");
    }

    public void reloadPluginState() {
        reloadConfig();
        debugEnabled = getConfig().getBoolean("debug", false);
    }

    public PlayerSettingsStore getPlayerSettingsStore() {
        return playerSettingsStore;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean toggleDebugEnabled() {
        debugEnabled = !debugEnabled;
        getConfig().set("debug", Boolean.valueOf(debugEnabled));
        saveConfig();
        return debugEnabled;
    }

    public void debug(String message) {
        if (debugEnabled) {
            getLogger().info("[debug] " + message);
        }
    }

    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }

    public String message(String path, String fallback) {
        String prefix = getConfig().getString("messages.prefix", "&6[TreeCutter]&r ");
        String body = getConfig().getString(path, fallback);
        return colorize(prefix + body);
    }

    private void registerCommand(String name, TreeCutterCommand handler) {
        PluginCommand command = getCommand(name);
        if (command != null) {
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        } else {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml.");
        }
    }
}
