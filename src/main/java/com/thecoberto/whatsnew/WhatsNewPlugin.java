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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * What's New: a menu of what changed in the latest server update, read from
 * plugins/WhatsNew/whats-new.md. Players see it once when they join after an update
 * (until they close it) and any time with /whatsnew. Changing the file's "update:" line
 * makes it show again for everyone. The file is re-read whenever it changes.
 */
public final class WhatsNewPlugin extends JavaPlugin implements Listener {
	static final Component PREFIX = Component.text("[", NamedTextColor.DARK_GRAY)
			.append(Component.text("What's New", NamedTextColor.GOLD))
			.append(Component.text("] ", NamedTextColor.DARK_GRAY));

	private NamespacedKey seenKey;
	private Path file;
	private long loadedModified = Long.MIN_VALUE;
	private WhatsNewDoc doc = WhatsNewDoc.parse("");
	private JavaMenu javaMenu;
	private BedrockMenu bedrockMenu;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		seenKey = new NamespacedKey(this, "seen");
		file = getDataFolder().toPath().resolve("whats-new.md");
		if (!Files.exists(file)) saveResource("whats-new.md", false);
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

	// ---------- The file ----------

	/** The latest whats-new.md, re-read if it changed on disk. */
	WhatsNewDoc doc() {
		try {
			long modified = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0;
			if (modified != loadedModified) reload();
		} catch (IOException e) {
			getLogger().warning("Couldn't check whats-new.md: " + e.getMessage());
		}
		return doc;
	}

	private void reload() {
		try {
			loadedModified = Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0;
			doc = WhatsNewDoc.parse(Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : "");
		} catch (IOException e) {
			getLogger().warning("Couldn't read whats-new.md: " + e.getMessage());
		}
	}

	private String summary() {
		WhatsNewDoc d = doc;
		if (d.isEmpty()) return "whats-new.md is empty, so nothing is shown.";
		return "Update \"" + d.update + "\": " + d.title + ", " + d.pages.size() + " page(s).";
	}

	// ---------- Seen ----------

	boolean hasSeen(Player player, WhatsNewDoc d) {
		return d.update.equals(player.getPersistentDataContainer().get(seenKey, PersistentDataType.STRING));
	}

	void markSeen(Player player, WhatsNewDoc d) {
		if (player.isOnline()) player.getPersistentDataContainer().set(seenKey, PersistentDataType.STRING, d.update);
	}

	// ---------- Showing it ----------

	void open(Player player, int screen) {
		WhatsNewDoc d = doc();
		if (d.isEmpty()) {
			player.sendMessage(PREFIX.append(Component.text("Nothing new right now.", NamedTextColor.GRAY)));
			return;
		}
		if (bedrockMenu != null && bedrockMenu.isBedrock(player)) bedrockMenu.open(player, d, screen);
		else javaMenu.open(player, d, screen);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (!getConfig().getBoolean("show-on-join", true)) return;
		Player player = event.getPlayer();
		WhatsNewDoc d = doc();
		if (d.isEmpty() || hasSeen(player, d)) return;

		// Brand-new players already get the welcome menus; the update news is old news to them
		if (!player.hasPlayedBefore() && !getConfig().getBoolean("show-to-new-players", false)) {
			markSeen(player, d);
			return;
		}

		long delay = Math.max(1, getConfig().getLong("join-delay-seconds", 4) * 20);
		getServer().getScheduler().runTaskLater(this, () -> {
			WhatsNewDoc now = doc();
			if (player.isOnline() && !now.isEmpty() && !hasSeen(player, now)) open(player, -1);
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
			doc();
			sender.sendMessage(PREFIX.append(Component.text(summary())));
			return true;
		}
		open(player, -1);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1 && sender.hasPermission("whatsnew.admin") && "reload".startsWith(args[0].toLowerCase())) {
			return List.of("reload");
		}
		return List.of();
	}
}
