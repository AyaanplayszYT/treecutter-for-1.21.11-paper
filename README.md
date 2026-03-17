# Mistix TreeCutter (Paper 1.21.x)

Sneak + break bottom log = whole tree breaks.
Includes anti-abuse cooldown and max tree block limit.

## Features
- Trigger only while crouching
- Requires breaking the bottom log (configurable)
- Optional axe requirement
- Per-player cooldown to prevent abuse
- Max blocks per tree cap

## Permissions

- `mistix.treecutter.use` (default: true)
- `mistix.treecutter.bypasscooldown` (default: op)

## Default config
See [src/main/resources/config.yml](src/main/resources/config.yml).

```yml
cooldown-seconds: 8
max-blocks-per-tree: 180
require-axe: true
require-bottom-log: true
messages:
	cooldown: "§cTree cutter cooldown: %seconds%s"
	no-permission: "§cYou don't have permission to use tree cutter."
```

## Build
```powershell
$env:JAVA_HOME='C:\path\to\jdk-21.0.8'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean build
```

Output jar:
- `build/libs/mistix-treecutter-paper-1.0.0.jar`

## Install

1. Put jar into server `plugins` folder
2. Start server once to generate config
3. Edit config if needed
4. Restart server

## License

MIT License. See `LICENSE`.
