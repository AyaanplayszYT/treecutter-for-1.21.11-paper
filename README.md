<div align="center">

# TreeCutter

[![Available for Bukkit Family](https://img.shields.io/badge/Bukkit%2FSpigot%2FPaper%2FPurpur-supported-3a7?style=for-the-badge)](https://modrinth.com/plugin/simpletreecutter)
[![Available on Modrinth](https://raw.githubusercontent.com/Fluxoah/Banners/main/available_on_modrinth.png)](https://modrinth.com/plugin/simpletreecutter)

TreeCutter is a lightweight and configurable Minecraft plugin that lets players cut down entire natural trees with rich admin controls, player toggles, particles, sounds, and flexible activation modes.

**Made by Mistix**

</div>

---

## Features

- Full tree cutting with hand or configured tools.
- Commands for reload, status, toggle, debug, and help.
- Per-player enable or disable support.
- Configurable worlds, tools, log types, cooldown tiers, particles, and sounds.
- Optional leaf breaking and shift-left-click activation mode.
- Single JAR targeting Bukkit-family servers (`Spigot`, `Paper`, `Purpur`) from `1.13+`.

---

## Installation

1. Place `mistix-simpletreecutter-2.0.0.jar` in your server `plugins` folder.
2. Start the server once to generate the default config files.
3. Adjust `config.yml` to match your server rules.
4. Use `/treecutter reload` after config changes.

---

## Commands

- `/treecutter reload`
- `/treecutter status [player]`
- `/treecutter toggle [player]`
- `/treecutter debug`
- `/treecutterhelp`

---

## Config Highlights

- Enable or disable the plugin in specific worlds.
- Allow hand cutting or restrict usage to selected tools.
- Whitelist or blacklist log materials.
- Require leaves, break leaves with the tree, and set tree size limits.
- Choose between normal break mode and shift-left-click mode.
- Configure cooldown tiers by permission.
- Add particle and sound feedback for every cut.

---

## Compatibility

- Platforms: Bukkit-family servers (`Spigot`, `Paper`, `Purpur`, and forks compatible with Bukkit API).
- Minecraft versions: `1.13+`.
- Notes: This plugin avoids Paper-only APIs to maximize compatibility.

---

## Links

- Modrinth: https://modrinth.com/plugin/simpletreecutter
- MC Username: Mistix9811
