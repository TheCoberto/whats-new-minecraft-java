# What's New (Paper plugin)

A menu of what changed in the latest server update. Players see it once when they join after an update, page through it, and close it. It stays closed until the next update. `/whatsnew` opens it any time.

- **Java players** get a pop-up screen (a dialog, Minecraft 1.21.6 and newer).
- **Bedrock players** joining through Geyser get a Bedrock form, when [Floodgate](https://geysermc.org/wiki/floodgate/) is installed.
- The first screen is the update's title, an intro and a button for each page (plus **Start reading**). Each page has **Next: <page>**, **Contents** and **Close** (**Done** on the last page).
- Pressing Close or Done (or the X on Bedrock) marks this update as read for that player. Java players can't close it with Esc, so they don't skip it by accident.
- Players joining for the very first time are marked as having read it (they have welcome menus to get through), unless `show-to-new-players` is on.

Needs Paper 26.3 or newer and Java 25. Floodgate is optional.

## The text

`plugins/WhatsNew/whats-new.md`, re-read automatically whenever it changes, so no restart or reload is needed. [paper-server](https://github.com/TheCoberto/paper-server)'s `publish-whats-new.ps1` uploads it.

```markdown
# The Big Update                     <- the menu's title
update: 2026-11-24                   <- change this and everyone sees the menu again

Intro text, on the first screen.
> Grey note

## New Plugins                       <- each ## starts a page
### Small heading
- A bullet with **bold** and `a command`
<!-- left out of the menu -->
```

Without an `update:` line the title is used, so a new title also counts as a new update. Fixing a typo without changing `update:` doesn't show the menu again to players who closed it. The Bedrock add-on ([whats-new](https://github.com/TheCoberto/whats-new)) reads the same format.

## Commands and settings

- `/whatsnew` (or `/updates`) opens the menu. From the console it prints which update is loaded.
- `/whatsnew reload` (`whatsnew.admin`, ops) re-reads the file right away.

`config.yml`: `show-on-join`, `join-delay-seconds` (default 4) and `show-to-new-players` (default false).

Which update a player has read is saved on the player (`whatsnew:seen`).

## Build
```
./gradlew build
```
Needs JDK 25. The jar ends up in `build/libs/`.
