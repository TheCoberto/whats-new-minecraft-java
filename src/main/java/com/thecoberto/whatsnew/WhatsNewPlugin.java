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
import java.util.List;

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

	boolean hasSeen(Player player, WhatsNewDoc d) {
		return d.update.equals(player.getPersistentDataContainer().get(seenKey, PersistentDataType.STRING));
	}

	/** Closing the menu, on any screen, counts as having read the current update. */
	void markSeen(Player player) {
		Updates u = updates();
		if (player.isOnline() && !u.isEmpty()) {
			player.getPersistentDataContainer().set(seenKey, PersistentDataType.STRING, u.latest().update);
		}
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
		if (u.isEmpty() || hasSeen(player, u.latest())) return;

		// Brand-new players already get the welcome menus; the update news is old news to them
		if (!player.hasPlayedBefore() && !getConfig().getBoolean("show-to-new-players", false)) {
			markSeen(player);
			return;
		}

		long delay = Math.max(1, getConfig().getLong("join-delay-seconds", 4) * 20);
		getServer().getScheduler().runTaskLater(this, () -> {
			Updates now = updates();
			if (player.isOnline() && !now.isEmpty() && !hasSeen(player, now.latest())) openLatest(player);
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
