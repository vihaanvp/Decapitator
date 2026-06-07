# Decapitator

Decapitator is a simple Bukkit/Paper plugin that gives players player heads via command. It supports per-player limits (persisted to disk), runtime config reloading, and optional integration with SkinsRestorer for proper skins on offline-mode servers. If SkinsRestorer is not present, the plugin falls back to the server-side PlayerProfile behavior.

## Features
- /head command to give player heads (self or others).
- Per-player limit (configurable, persisted to plugins/Decapitator/playercounts.yml).
- Unlimited mode using max-heads-per-player: -1.
- Detects and uses SkinsRestorer (if installed) for correct skins on offline servers.
- Reload command to reload config and stored player data at runtime.
- Simple permissions for command control.

## Commands
- /head
    - Give your own head.
- /head <player>
    - Give another player's head (requires permission `decapitator.head.others`).
- /head count
    - Show how many heads you have ordered.
- /head count <player>
    - Show how many heads another player has ordered (requires `decapitator.count.others`).
- /head reload
    - Reload config and playercounts from disk (requires `decapitator.reload`).
- /head help
    - Show usage help.

## Permissions
- decapitator.head — Use /head to get your own head (default: true)
- decapitator.head.others — Get another player's head (default: op)
- decapitator.count.others — View other players' head counts (default: op)
- decapitator.reload — Reload config and player data (default: op)

Adjust permissions in your permissions plugin if needed.

## Configuration
Default config keys (plugins/Decapitator/config.yml or src/main/resources/config.yml):

```yaml
# Decapitator configuration
enabled: true

# Maximum number of heads a player can order (per-player limit).
# Set to -1 for unlimited heads.
max-heads-per-player: -1
```

- enabled: false will disable the command logic (command still exists and will report disabled).
- max-heads-per-player: set to -1 for no limit (unlimited).

After changing the file manually you can reload using `/head reload` (requires permission) or restart the server.

## Data storage
Per-player counts are stored in:
- plugins/Decapitator/playercounts.yml

Format is simple mapping from UUID to integer:
```yaml
"uuid-string-here": 3
```

The plugin creates and maintains this file automatically. Use `/head count` to view counts and (optionally) admin commands to reset counts if you add them later.

## Skins and Offline Mode
- If the SkinsRestorer plugin is installed and enabled, Decapitator will attempt to obtain skin texture data from SkinsRestorer and apply it to the head item. This provides correct skins on offline-mode servers if SkinsRestorer manages those skins.
- If SkinsRestorer is not present, the plugin falls back to setting the skull's owning player (SkullMeta#setOwningPlayer). That works best on online-mode servers where Mojang skins/profiles are available or for currently-online players whose client-provided profile contains skin data.
- Note: true offline accounts that never existed on Mojang and are not managed by SkinsRestorer cannot have Mojang skin textures resolved.

## Installation

Preferred — Download from Modrinth (please)
1. Download the latest Decapitator JAR from the Modrinth page (according to your server version).
2. Place the downloaded JAR in your server's `plugins/` folder (e.g. `plugins/Decapitator.jar`).
3. Start the server — the plugin will create `plugins/Decapitator/config.yml` and `playercounts.yml` on first run.
4. Configure `config.yml` as desired and optionally run `/head reload` to reload without restarting.

Alternative — Download from Releases tab (not recommended)
- You can also download the JAR from the project's Releases tab on GitHub, but please consider supporting the creator by downloading from Modrinth instead.
- After downloading from Releases, follow steps 2–4 above.

## Notes for Integrators
- The plugin uses reflection to integrate with SkinsRestorer at runtime so it does not require a compile-time dependency on the SkinsRestorer API. If you want a compile-time typed integration, add SkinsRestorer API as an optional dependency and adapt the code to call the API directly.
- The plugin also uses reflection to inject GameProfile texture data into skull metadata if a texture value and signature are available (SkinsRestorer or other providers).

## Contributing
Contributions are welcome. Suggested enhancements:
- Admin commands to reset or modify per-player counts.
- Optional caching layer for external skin lookups.
- Tab completion for subcommands and player names.