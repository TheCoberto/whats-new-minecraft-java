# Changelog

## 1.2.0

- **New pages pop up on their own**: adding a page (`##`) to the current update shows it to players who already read that update. The menu opens straight on the first new page, and new pages are marked **New:** in the contents and on the page. No need to change the `update:` line any more
- Editing an existing page (a typo, a reworded bullet) doesn't show anything again; pages are matched by their title
- A new update file still pops up in full, as before

## 1.1.0

- Update history: one file per update in plugins/WhatsNew/updates/ (named with its date first). The newest pops up on join; older ones are under **Past Updates**, newest first, with their dates
- `/whatsnew history` opens Past Updates; `history-limit` in config.yml sets how many are listed (default 10)
- A file without an `update:` line uses its file name as its id, so a new file is a new update
- The single whats-new.md from 1.0.0 is still read while the updates folder is empty

## 1.0.0

- First release: a What's New menu players see once after each server update, and any time with /whatsnew
- Java players get a pop-up screen; Bedrock players (through Geyser and Floodgate) get a Bedrock form
- Reads plugins/WhatsNew/whats-new.md and picks up changes to it without a restart
