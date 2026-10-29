# AutoSell (Fabric, Minecraft 26.2)

Client-side mod that sends a sell command whenever you pick up items.

## Build
Requires JDK 25.

    gradle wrapper --gradle-version 9.3.1   # once, to generate the wrapper (or copy it from the Fabric example mod)
    ./gradlew build

Jar ends up in `build/libs/`. Put it in `mods/` with Fabric Loader 0.19.3+ and Fabric API.

## Commands
| Command | What it does |
|---|---|
| `/autosell` | Toggle on/off |
| `/autosell status` | Show settings |
| `/autosell delay <ticks>` | Ticks between sells (20 = 1s, 0 = every tick) |
| `/autosell command <text>` | Command to send, e.g. `sell` (no slash needed) |
| `/autosell add <item>` / `remove <item>` / `clear` | Item filter. Empty = any item |

Config lives in `config/autosell.json`.

## Notes
- Use on servers whose rules allow automation. Many ban auto-clickers/macros, and spamming commands can get you kicked or banned.
- If `/sell` on your server opens a menu rather than selling instantly, this mod will not click through it.
