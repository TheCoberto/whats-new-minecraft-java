# What's New (Paper plugin)

A menu of what changed in each server update. Players see the newest one once when they join after it's added, page through it, and close it. It stays closed until the next update. `/whatsnew` opens it any time, and **Past Updates** lists the older ones.

- **Java players** get a pop-up screen (a dialog, Minecraft 1.21.6 and newer).
- **Bedrock players** joining through Geyser get a Bedrock form, when [Floodgate](https://geysermc.org/wiki/floodgate/) is installed.
- The first screen is the update's title, an intro and a button for each page (plus **Start reading**). Each page has **Next: <page>**, **Contents** and **Close** (**Done** on the last page).
- The current update's first screen also has **Past Updates**: a list of older updates, newest first, each with its date. Opening one shows its pages the same way, with a button back to the list.
- Pressing Close or Done (or the X on Bedrock), on any screen, marks the current update as read for that player. Java players can't close it with Esc, so they don't skip it by accident.
- Players joining for the very first time are marked as having read it (they have welcome menus to get through), unless `show-to-new-players` is on.

Needs Paper 26.3 or newer and Java 25. Floodgate is optional.

## The text

One file per update in `plugins/WhatsNew/updates/`, named with its date first:

```
plugins/WhatsNew/updates/
  2026-11-24-high-stakes.md     <- older: under Past Updates, dated Nov 24, 2026
  2026-12-20-holiday.md         <- newest by name: the current update, pops up on join
```

Files are sorted by name, so the date at the start decides which is newest. They're re-read automatically whenever one changes, so no restart or reload is needed. [paper-server](https://github.com/TheCoberto/paper-server)'s `publish-whats-new.ps1` uploads the folder.

```markdown
# The Big Update                     <- the menu's title
update: 2026-11-24                   <- optional: the update's id (the file name is used without it)

Intro text, on the first screen.
> Grey note

## New Plugins                       <- each ## starts a page
### Small heading
- A bullet with **bold** and `a command`
<!-- left out of the menu -->
```

A new file is a new update: everyone sees it once. Editing a file (a typo, say) doesn't show it again to players who closed it, unless its `update:` line changes. The Bedrock add-on ([whats-new](https://github.com/TheCoberto/whats-new)) reads the same format.

Upgrading from 1.0.0: the old single `plugins/WhatsNew/whats-new.md` is still used while the `updates` folder is empty. Keep its `update:` line when you move it into the folder, so players who already read it don't see it again.

## Commands and settings

- `/whatsnew` (or `/updates`) opens the current update. From the console it prints which update is loaded and how many past ones there are.
- `/whatsnew history` opens Past Updates.
- `/whatsnew reload` (`whatsnew.admin`, ops) re-reads the files right away.

`config.yml`: `show-on-join`, `join-delay-seconds` (default 4), `show-to-new-players` (default false) and `history-limit` (how many past updates are listed, default 10).

Which update a player has read is saved on the player (`whatsnew:seen`).

## Build
```
./gradlew build
```
Needs JDK 25. The jar ends up in `build/libs/`.
