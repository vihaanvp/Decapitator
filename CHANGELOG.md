# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
- Planned: add admin reset command for player counts.
- Planned: add tab-completion for subcommands and player names.
- Planned: optional caching layer for skin lookups.

---

## [1.1.0] - 2026-06-07
### Added
- Per-player persistent usage limits for /head (stored in plugins/Decapitator/playercounts.yml).
- Config support:
    - `enabled` to toggle plugin behaviour.
    - `max-heads-per-player` to set per-player limit (use `-1` for unlimited).
- `/head count [player]` to view how many heads a player has ordered.
- `/head reload` to reload the plugin config and player data at runtime.
- Automatic creation and management of `playercounts.yml` in the plugin data folder.
- Integration with SkinsRestorer:
    - If SkinsRestorer is installed and enabled, Decapitator will request skin data from SkinsRestorer and apply textures so heads show proper skins on offline-mode servers.
    - Reflection-based integration so SkinsRestorer is optional at runtime (no compile-time dependency).
- README and documented permissions.

### Changed
- Command behaviour extended to include subcommands (`count`, `reload`, `help`) while preserving the original `/head [player]` functionality.
- Messages enhanced to show current count and max (shows "∞" when unlimited).

### Fixed
- Ensure head-giving never crashes if SkinsRestorer integration fails — falls back to server-side PlayerProfile behaviour.

### Notes
- New permissions added: `decapitator.count.others`, `decapitator.reload`. See README for full list.
- Data file format: YAML mapping of UUID -> integer (e.g., `"uuid-string": 3`).

---

## [1.0.0] - 2026-06-06
### Added
- Initial plugin implementation:
    - `/head` and `/head <player>` to give player heads.
    - Basic SkullMeta usage via `SkullMeta#setOwningPlayer`.
    - `plugin.yml` with command and base permissions.
    - Maven pom.xml sample and basic project structure.
    - Basic README.

### Notes
- Heads for offline-mode servers without SkinsRestorer will typically show default skins because Mojang profile data is not available.

---

## Upgrade / Migration notes
- After upgrading to 1.1.0:
    - The plugin will create `plugins/Decapitator/playercounts.yml`. Existing users who previously used `/head` will begin with a count of 0 unless you manually add entries.
    - If you want unlimited heads, set `max-heads-per-player: -1` in `plugins/Decapitator/config.yml` (or in the bundled `src/main/resources/config.yml`) and run `/head reload` or restart the server.
    - If you relied on third-party skin management (e.g., SkinsRestorer), ensure that plugin is installed and enabled for Decapitator to use it automatically. No additional configuration is required to enable SkinsRestorer support.

## Credits
- Developed by: vihaanvp (using GitHub Copilot)
- Skins integration support thanks to SkinsRestorer (optional plugin)