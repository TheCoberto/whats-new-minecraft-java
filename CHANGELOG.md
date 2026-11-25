# Changelog

## 1.1.0

- Update history: one file per update in plugins/WhatsNew/updates/ (named with its date first). The newest pops up on join; older ones are under **Past Updates**, newest first, with their dates
- `/whatsnew history` opens Past Updates; `history-limit` in config.yml sets how many are listed (default 10)
- A file without an `update:` line uses its file name as its id, so a new file is a new update
- The single whats-new.md from 1.0.0 is still read while the updates folder is empty

## 1.0.0

- First release: a What's New menu players see once after each server update, and any time with /whatsnew
- Java players get a pop-up screen; Bedrock players (through Geyser and Floodgate) get a Bedrock form
- Reads plugins/WhatsNew/whats-new.md and picks up changes to it without a restart
