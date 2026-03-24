package dev.mistix.treecutter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TreeCutterCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = Arrays.asList("help", "reload", "status", "toggle", "debug");

    private final MistixTreeCutterPlugin plugin;

    public TreeCutterCommand(MistixTreeCutterPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("treecutterhelp")) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission("mistix.treecutter.command")) {
            sender.sendMessage(plugin.message("messages.no-permission", "&cYou do not have permission to use TreeCutter."));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("mistix.treecutter.admin.reload")) {
                sender.sendMessage(plugin.message("messages.no-permission", "&cYou do not have permission to do that."));
                return true;
            }

            plugin.reloadPluginState();
            sender.sendMessage(plugin.message("messages.reload", "&aTreeCutter configuration reloaded."));
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            handleStatus(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            handleToggle(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("mistix.treecutter.admin.debug")) {
                sender.sendMessage(plugin.message("messages.no-permission", "&cYou do not have permission to do that."));
                return true;
            }

            boolean enabled = plugin.toggleDebugEnabled();
            sender.sendMessage(plugin.message("messages.debug", "&eDebug mode is now %state%.")
                    .replace("%state%", enabled ? "enabled" : "disabled"));
            return true;
        }

        sendHelp(sender);
        return true;
    }

    private void handleStatus(CommandSender sender, String[] args) {
        Player target = sender instanceof Player ? (Player) sender : null;

        if (args.length > 1) {
            if (!sender.hasPermission("mistix.treecutter.admin.status")) {
                sender.sendMessage(plugin.message("messages.no-permission", "&cYou do not have permission to do that."));
                return;
            }

            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.message("messages.player-not-found", "&cThat player is not online."));
                return;
            }
        }

        if (target == null) {
            sender.sendMessage(plugin.colorize("&6TreeCutter Status"));
            sender.sendMessage(plugin.colorize("&7Debug: &f" + plugin.isDebugEnabled()));
            sender.sendMessage(plugin.colorize("&7Activation mode: &f" + plugin.getConfig().getString("activation-mode", "BREAK")));
            sender.sendMessage(plugin.colorize("&7Sneak required: &f" + plugin.getConfig().getBoolean("sneak-required", true)));
            return;
        }

        boolean enabled = plugin.getPlayerSettingsStore().isEnabled(target.getUniqueId());
        sender.sendMessage(plugin.message("messages.status", "&eTreeCutter for %player% is %state%.")
                .replace("%player%", target.getName())
                .replace("%state%", enabled ? "enabled" : "disabled"));
    }

    private void handleToggle(CommandSender sender, String[] args) {
        Player target;

        if (args.length > 1) {
            if (!sender.hasPermission("mistix.treecutter.admin.toggle.other")) {
                sender.sendMessage(plugin.message("messages.no-permission", "&cYou do not have permission to do that."));
                return;
            }

            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.message("messages.player-not-found", "&cThat player is not online."));
                return;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.message("messages.console-toggle-target", "&cConsole must specify a player."));
                return;
            }
            target = (Player) sender;
        }

        boolean enabled = plugin.getPlayerSettingsStore().toggle(target.getUniqueId());
        target.sendMessage(plugin.message("messages.toggle-self", "&eTreeCutter is now %state%.")
                .replace("%state%", enabled ? "enabled" : "disabled"));

        if (!target.equals(sender)) {
            sender.sendMessage(plugin.message("messages.toggle-other", "&eTreeCutter for %player% is now %state%.")
                    .replace("%player%", target.getName())
                    .replace("%state%", enabled ? "enabled" : "disabled"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.colorize("&6TreeCutter Commands"));
        sender.sendMessage(plugin.colorize("&e/treecutter reload &7- Reload the plugin configuration"));
        sender.sendMessage(plugin.colorize("&e/treecutter status [player] &7- Show TreeCutter status"));
        sender.sendMessage(plugin.colorize("&e/treecutter toggle [player] &7- Toggle TreeCutter"));
        sender.sendMessage(plugin.colorize("&e/treecutter debug &7- Toggle debug mode"));
        sender.sendMessage(plugin.colorize("&e/treecutterhelp &7- Show this help message"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("treecutterhelp")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("toggle") || args[0].equalsIgnoreCase("status"))) {
            List<String> names = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filter(names, args[1]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        List<String> matches = new ArrayList<String>();
        String loweredInput = input.toLowerCase();
        for (String value : values) {
            if (value.toLowerCase().startsWith(loweredInput)) {
                matches.add(value);
            }
        }
        return matches;
    }
}