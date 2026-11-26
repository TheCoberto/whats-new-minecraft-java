package com.thecoberto.whatsnew;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What's New: a menu of what changed in each server update, one file per update in
 * plugins/WhatsNew/updates/. The newest is shown once when players join after it's added
 * (until they close it) and any time with /whatsnew; older ones are under Past Updates.
 * The folder is re-read whenever a file in it changes.
 */
public final class WhatsNewPlugin extends JavaPlugin implements Listener {
	static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
			.append(Component.text("What's New", NamedTextColor.GOLD))
			.append(Component.text("] ", NamedTextColor.DARK_GRAY));

	/** Screens: the contents of an update, the Past Updates list; 0 and up are pages. */
	static final int CONTENTS = -1, PAST = -2;

	private NamespacedKey seenKey;
	/** The page titles of that update the player had when they closed it, one per line. */
	private NamespacedKey pagesKey;
	private Path dir;
	private Path legacy;
	private String loadedSignature;
	private Updates updates = Updates.EMPTY;
	private JavaMenu javaMenu;
	private BedrockMenu bedrockMenu;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		seenKey = new NamespacedKey(this, "seen");
		pagesKey = new NamespacedKey(this, "seen_pages");
		dir = getDataFolder().toPath().resolve("updates");
		legacy = getDataFolder().toPath().resolve("whats-new.md");
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			getLogger().warning("Couldn't make the updates folder: " + e.getMessage());
		}
		javaMenu = new JavaMenu(this);
		if (getServer().getPluginManager().getPlugin("floodgate") != null) {
			try {
				bedrockMenu = new BedrockMenu(this);
			} catch (LinkageError e) {
				getLogger().warning("Floodgate is installed but its API couldn't be loaded; Bedrock players get the Java menu: " + e);
			}
		}
		reload();
		getServer().getPluginManager().registerEvents(this, this);
		getLogger().info(summary());
	}

	// ---------- The files ----------

	/** Every update, re-read if a file changed. */
	Updates updates() {
		if (!Updates.signature(dir, legacy).equals(loadedSignature)) reload();
		return updates;
	}

	private void reload() {
		loadedSignature = Updates.signature(dir, legacy);
		updates = Updates.load(dir, legacy, getLogger());
	}

	int historyLimit() {
		return getConfig().getInt("history-limit", 10);
	}

	private String summary() {
		Updates u = updates;
		if (u.isEmpty()) return "No updates in plugins/WhatsNew/updates, so nothing is shown.";
		WhatsNewDoc d = u.latest();
		return "Update \"" + d.update + "\": " + d.title + ", " + d.pages.size() + " page(s); "
				+ (u.all.size() - 1) + " past update(s).";
	}

	// ---------- Seen ----------

	/** Whether the player has closed this update's menu (pages added since then don't matter here). */
	boolean hasSeen(Player player, WhatsNewDoc d) {
		return d.update.equals(player.getPersistentDataContainer().get(seenKey, PersistentDataType.STRING));
	}

	/**
	 * Pages of the current update added since the player last closed it, by index. Empty when
	 * they haven't seen this update at all (the whole thing is new, so nothing is singled out),
	 * when doc isn't the current update, or when they closed it before 1.2.0 (no page list saved).
	 */
	Set<Integer> newPages(Player player, WhatsNewDoc d) {
		Updates u = updates();
		if (u.isEmpty() || !u.isLatest(d) || !hasSeen(player, d)) return Set.of();
		String saved = player.getPersistentDataContainer().get(pagesKey, PersistentDataType.STRING);
		if (saved == null) return Set.of();
		Set<String> seen = new HashSet<>(List.of(saved.split("\n")));
		Set<Integer> out = new LinkedHashSet<>();
		for (int i = 0; i < d.pages.size(); i++) {
			if (!seen.contains(pageKey(d.pages.get(i)))) out.add(i);
		}
		return out;
	}

	/** A page is known by its title, so editing its text (a typo) doesn't make it new again. */
	private static String pageKey(WhatsNewDoc.Page page) {
		return page.title().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
	}

	/** Closing the menu, on any screen, counts as having read the current update and all its pages. */
	void markSeen(Player player) {
		Updates u = updates();
		if (player.isOnline() && !u.isEmpty()) {
			WhatsNewDoc latest = u.latest();
			var data = player.getPersistentDataContainer();
			data.set(seenKey, PersistentDataType.STRING, latest.update);
			data.set(pagesKey, PersistentDataType.STRING,
					String.join("\n", latest.pages.stream().map(WhatsNewPlugin::pageKey).toList()));
		}
	}

	/** Something to show on join: the whole update, or the pages added since they last read it. */
	private boolean hasNews(Player player, WhatsNewDoc d) {
		return !hasSeen(player, d) || !newPages(player, d).isEmpty();
	}

	// ---------- Showing it ----------

	/** Opens a screen (CONTENTS, PAST or a page) of an update. */
	void open(Player player, WhatsNewDoc doc, int screen) {
		if (bedrockMenu != null && bedrockMenu.isBedrock(player)) bedrockMenu.open(player, doc, screen);
		else javaMenu.open(player, doc, screen);
	}

	void openLatest(Player player) {
		Updates u = updates();
		if (u.isEmpty()) {
			player.sendMessage(PREFIX.append(Component.text("Nothing new right now.", NamedTextColor.GRAY)));
			return;
		}
		open(player, u.latest(), CONTENTS);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (!getConfig().getBoolean("show-on-join", true)) return;
		Player player = event.getPlayer();
		Updates u = updates();
		if (u.isEmpty() || !hasNews(player, u.latest())) return;

		// Brand-new players already get the welcome menus; the update news is old news to them
		if (!player.hasPlayedBefore() && !getConfig().getBoolean("show-to-new-players", false)) {
			markSeen(player);
			return;
		}

		long delay = Math.max(1, getConfig().getLong("join-delay-seconds", 4) * 20);
		getServer().getScheduler().runTaskLater(this, () -> {
			Updates now = updates();
			if (!player.isOnline() || now.isEmpty()) return;
			WhatsNewDoc latest = now.latest();
			if (!hasSeen(player, latest)) {
				openLatest(player);
			} else {
				// Pages added to the update they already read: open on the first new one
				Set<Integer> fresh = newPages(player, latest);
				if (!fresh.isEmpty()) open(player, latest, fresh.iterator().next());
			}
		}, delay);
	}

	// ---------- /whatsnew ----------

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("whatsnew.admin")) {
				sender.sendMessage(PREFIX.append(Component.text("You can't do that.", NamedTextColor.RED)));
				return true;
			}
			reload();
			sender.sendMessage(PREFIX.append(Component.text("Reloaded. " + summary(), NamedTextColor.GREEN)));
			return true;
		}
		if (!(sender instanceof Player player)) {
			updates();
			sender.sendMessage(PREFIX.append(Component.text(summary())));
			return true;
		}
		if (args.length > 0 && (args[0].equalsIgnoreCase("history") || args[0].equalsIgnoreCase("past"))) {
			Updates u = updates();
			if (u.past(historyLimit()).isEmpty()) {
				player.sendMessage(PREFIX.append(Component.text("No past updates yet.", NamedTextColor.GRAY)));
				openLatest(player);
			} else {
				open(player, u.latest(), PAST);
			}
			return true;
		}
		openLatest(player);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length != 1) return List.of();
		String typed = args[0].toLowerCase();
		List<String> options = sender.hasPermission("whatsnew.admin") ? List.of("history", "reload") : List.of("history");
		return options.stream().filter(o -> o.startsWith(typed)).toList();
	}
}
