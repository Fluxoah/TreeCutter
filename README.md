<div align="center">

# TreeCutter
[![Ko-fi](https://raw.githubusercontent.com/Fluxoah/Banners/main/available_for_paper.png)](https://modrinth.com/plugin/tree-cutter+/versions?l=paper)
[![Ko-fi](https://raw.githubusercontent.com/Fluxoah/Banners/main/available_for_pupur.png)](https://modrinth.com/plugin/tree-cutter+/versions?l=purpur)
[![Ko-fi](https://raw.githubusercontent.com/Fluxoah/Banners/main/available_on_modrinth.png)](https://modrinth.com/plugin/tree-cutter+)

TreeCutter is a lightweight and configurable Minecraft plugin that allows players to cut down entire natural trees instantly by sneaking and breaking the bottom log. Tool durability is respected, commands are LuckPerms-friendly, and admins have full control.

</div>

---

## Features

- Instant tree cutting when sneaking and breaking the bottom log.
- Tool durability handling with Unbreaking support.
- Durability break confirmation (crouch + break again) with silent confirmed break.
- Player-made tree blocking with a dedicated toggle: `behavior.block-player-made-trees`.
- Tracking for player-placed and command-placed logs (`setblock` / `fill`) with configurable max fill volume.
- WorldGuard compatibility with strict block-break checks and optional plugin flag support.
- Blacklist system with action-bar warnings and `/tcmsg off` to hide blacklist warnings.
- Unblacklist action-bar notice, including delivery when the player joins later.
- Prefix and messages fully configurable through `config.yml` and `messages.yml`.
- Compatible with Bukkit-derived servers including Spigot, Paper, and Purpur on the 26.1 update line.

---

## Installation

1. Download `TreeCutter.jar`.
2. Place it in the server's `plugins` folder.
3. Start the server to generate default config files:
`config.yml`
`messages.yml`
4. Configure options as needed.
5. Restart the server (or run `/tcadmin reload`) to apply changes.

For Minecraft 26.1 servers, run the server on Java 25.
Building the plugin itself only requires Java 21+ on this branch.

---

## Commands

### Player Commands

| Command        | Permission                    | Description                                 |
|----------------|-------------------------------|---------------------------------------------|
| `/tc on`       | `treecutter.command.tc.on`    | Enable TreeCutter for the player            |
| `/tc off`      | `treecutter.command.tc.off`   | Disable TreeCutter for the player           |
| `/tc status`   | `treecutter.command.tc.status`| Show player status                          |
| `/tc help`     | `treecutter.command.tc.help`  | Show available player commands              |
| `/tcmsg off`   | `treecutter.command.tcmsg`    | Hide blacklist action-bar warnings          |

### Admin Commands

| Command                             | Permission                         | Description                                   |
|-------------------------------------|------------------------------------|-----------------------------------------------|
| `/tcadmin on`                       | `treecutter.command.tcadmin.on`    | Enable plugin globally                        |
| `/tcadmin off`                      | `treecutter.command.tcadmin.off`   | Disable plugin globally                       |
| `/tcadmin reload`                   | `treecutter.command.tcadmin.reload`| Reload config and messages                    |
| `/tcadmin check <player>`           | `treecutter.command.tcadmin.check` | View player stats                             |
| `/tcadmin blacklist add <player>`   | `treecutter.command.tcadmin.blacklist` | Add player to blacklist                   |
| `/tcadmin blacklist remove <player>`| `treecutter.command.tcadmin.blacklist` | Remove player from blacklist               |
| `/tcadmin blacklist list`           | `treecutter.command.tcadmin.blacklist` | List blacklisted players                  |
| `/tcadmin help`                     | `treecutter.command.tcadmin.help`  | Show admin commands                           |

> Admin commands are only visible to users with permission.

---

## Permissions

- `treecutter.use` -> Allows cutting trees
- `treecutter.command.tc` -> Base access to `/tc`
- `treecutter.command.tcmsg` -> Access to `/tcmsg`
- `treecutter.command.tc.on` -> Access to `/tc on`
- `treecutter.command.tc.off` -> Access to `/tc off`
- `treecutter.command.tc.status` -> Access to `/tc status`
- `treecutter.command.tc.help` -> Access to `/tc help`
- `treecutter.command.tcadmin` -> Base access to `/tcadmin`
- `treecutter.command.tcadmin.on` -> Access to `/tcadmin on`
- `treecutter.command.tcadmin.off` -> Access to `/tcadmin off`
- `treecutter.command.tcadmin.reload` -> Access to `/tcadmin reload`
- `treecutter.command.tcadmin.blacklist` -> Access to blacklist admin actions
- `treecutter.command.tcadmin.check` -> Access to `/tcadmin check`
- `treecutter.command.tcadmin.help` -> Access to `/tcadmin help`
- `treecutter.bypass.worldguard` -> Bypass WorldGuard checks

> Permissions are fully enforced with LuckPerms. OP fallback only applies if LuckPerms is not installed.

---

## Configuration Files

### `config.yml`

<details>
<summary><strong>Configs</strong></summary>

<br>

```yaml
enabled: true

disabled-players: []
blacklist: []
blacklist-message-disabled: []
pending-unblacklist-notice: []
prefix: "&8[&3T&bC&8]"

behavior:
  sneak-required: true
  block-player-made-trees: true
  require-axe: false
  max-tree-size: 256
  cooldown-seconds: 1.0
  confirmation-required: true
  confirmation-time-seconds: 10

  tree-detection:
    max-leaf-search-radius: 3
    max-leaf-search-up: 5
    min-leaf-count: 4
    max-horizontal-spread: 6
    require-upward-growth: true

placed-log-tracking:
  player-placed: true
  command-placed: true
  max-fill-volume: 50000

notifications:
  blacklist-actionbar-seconds: 10
  unblacklist-actionbar-seconds: 10

drops:
  drop-items: true
  preserve-tool-durability: false
  allow-vanilla-drops: false

hooks:
  worldguard:
    enabled: true
    deny-partial-breaks: true
  placeholderapi:
    enabled: true
```

</details>

### `messages.yml`

- Fully customizable player/admin messages.
- Supports placeholder replacements (`{prefix}`, `{player}`, `{time}`, `{tool}`, `{count}`, etc).
- Blacklist action-bar warning text is configurable, including `/tcmsg off` hint.

---

## Notes

- `/tcmsg` does not show tab suggestions by design.
- If a player is unblacklisted while offline, the green unblacklist notice appears on next join.
- For region protection, keep `hooks.worldguard.enabled: true` and avoid giving `treecutter.bypass.worldguard` unless intended.
