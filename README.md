# Welcome Message

A small server-side Fabric mod for Minecraft.
Every joining player receives a personal welcome in chat.


## Installation

1. Put `welcome-message-<version>.jar` in the server's `mods` directory.
2. Start the server once. The mod creates `config/welcome-message.json`.
3. Edit that file and run `/welcomemessage reload`.

## Configuration

```json
{
  "enabled": true,
  "lines": [
    "&6Welcome, &e{player}&6!",
    "&7You are playing on our test server.",
    "",
    "&b[Visit our website](https://example.com)",
    "&a[Join our Discord](https://discord.com)",
    "&7Enjoy your stay!"
  ]
}
```

- `enabled`: turn automatic messages on or off.
- `lines`: add as many text strings as you need, in display order. Each string is a chat line; `""` makes a blank line. There is no two-line limit.
- `{player}`: replaced with the joining player's account name.
- `[visible text](https://example.com)`: clickable text with the URL shown on hover. Multiple links and ordinary text can share a line. Use absolute HTTP or HTTPS links. Encode parentheses in URLs as `%28` and `%29`. Link labels are plain text and inherit the formatting immediately before the link.
- Colors: `&0` black, `&1` dark blue, `&2` dark green, `&3` dark aqua, `&4` dark red, `&5` dark purple, `&6` gold, `&7` gray, `&8` dark gray, `&9` blue, `&a` green, `&b` aqua, `&c` red, `&d` light purple, `&e` yellow, `&f` white.
- Formatting: `&l` bold, `&o` italic, `&n` underline, `&m` strikethrough, `&k` obfuscated, `&r` reset. A color code also resets previous formatting. Each line starts with default formatting. Use `&&` for a literal ampersand.

JSON requires commas between lines, double quotes, and no trailing comma.
Escape a double quote inside a line as `\"`. Placeholder substitution affects visible text only, never link destinations.

## Commands and reload

- `/welcomemessage reload`: read the config immediately, without restarting.
- `/welcomemessage preview`: show the current welcome to yourself.
- Vanilla `/reload` also reloads the config after a successful data pack reload.

Mod commands require operator level 2 or higher. Reload also works from the server console, without the leading slash. Preview requires an in-game player and respects `enabled`.

A failed reload keeps the previous valid configuration active and reports the error in the console (and to the command sender for the dedicated reload command).
Existing files are never rewritten or reset. A missing file is only created during startup. If the initial config is invalid, welcomes stay disabled until you fix it and reload.
Reloading affects future joins and previews; it does not resend messages to everyone already online. The client's chat and link settings still determine whether messages and links are visible and usable.

## Build

Run `./gradlew build` (Windows: `.\gradlew.bat build`) with Java 25.
The installable jar is in `build/libs`; do not install the `-sources.jar`.
`build` includes configuration and text-rendering regression verification.
