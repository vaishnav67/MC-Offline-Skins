# Offline Skins for Minecraft 1.12.2 Forge

A client and server-side mod for Minecraft 1.12.2 (Forge 14.23.5.2847 - 14.23.5.2860) that restores skins in offline mode.

## TL;DR
- Set custom skins via official username or direct PNG URL.
- Server admins can force-set or clear any player's skin.
- Skins are saved to `custom_skins.json` in the world directory.

## Commands
### Users
- `/skin set <username|url> [default|slim]` - Set your skin
- `/skin clear` - Reset to default skin
- `/skin debug` - View your UUID and skin sync status
### Admin
- `/skin admin set <player> <username|url> [default|slim]` - Force-set a player's skin
- `/skin admin clear <player>` - Reset a player's skin

## Building from Source
Requires **Java 8 (JDK 1.8)**:

```bash
# Decompile workspace
./gradlew setupDecompWorkspace

# Build mod jar
./gradlew build

# Compiled .jar will be generated in build/libs/.
```