package com.thecoberto.whatsnew;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.thecoberto.whatsnew.WhatsNewPlugin.CONTENTS;
import static com.thecoberto.whatsnew.WhatsNewPlugin.PAST;

/**
 * The menu for Bedrock players joining through Geyser: a Bedrock form sent with Floodgate.
 * Only loaded when Floodgate is installed. Same screens as the Java menu, Past Updates
 * included; closing the form (X) or pressing Close/Done counts as having read it.
 */
final class BedrockMenu {
	private final WhatsNewPlugin plugin;

	BedrockMenu(WhatsNewPlugin plugin) {
		this.plugin = plugin;
	}

	boolean isBedrock(Player player) {
		return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
	}

	void open(Player player, WhatsNewDoc doc, int screen) {
		Updates updates = plugin.updates();
		if (screen == PAST) past(player, updates);
		else if (screen < 0 || screen >= doc.pages.size()) contents(player, updates, doc);
		else page(player, doc, screen);
	}

	private void contents(Player player, Updates updates, WhatsNewDoc doc) {
		boolean latest = updates.isLatest(doc);
		StringBuilder body = new StringBuilder();
		String date = updates.date(doc);
		if (!latest && !date.isEmpty()) body.append("§8").append(date).append("\n\n");
		body.append(Render.legacy(doc.intro));
		if (!doc.pages.isEmpty()) {
			if (!body.isEmpty()) body.append("\n\n");
			body.append("§7Pick a section, or start from the top.");
		}

		// What each button does, in order
		List<Runnable> actions = new ArrayList<>();
		SimpleForm.Builder form = SimpleForm.builder().title(doc.title).content(body.toString());
		if (!doc.pages.isEmpty()) {
			form.button("§2Start reading");
			actions.add(() -> open(player, doc, 0));
			Set<Integer> fresh = plugin.newPages(player, doc);
			for (int i = 0; i < doc.pages.size(); i++) {
				int page = i;
				form.button(fresh.contains(i) ? "§2§lNew:§r " + doc.pages.get(i).title() : doc.pages.get(i).title());
				actions.add(() -> open(player, doc, page));
			}
		}
		if (latest && !updates.past(plugin.historyLimit()).isEmpty()) {
			form.button("§3Past Updates");
			actions.add(() -> open(player, doc, PAST));
		} else if (!latest) {
			form.button("§3Back to Past Updates");
			actions.add(() -> open(player, doc, PAST));
		}
		form.button("Close");
		actions.add(() -> plugin.markSeen(player));
		send(player, form, actions);
	}

	private void past(Player player, Updates updates) {
		List<Runnable> actions = new ArrayList<>();
		SimpleForm.Builder form = SimpleForm.builder().title("Past Updates").content("§7Pick an update to read what it brought.");
		for (WhatsNewDoc d : updates.past(plugin.historyLimit())) {
			String date = updates.date(d);
			form.button(date.isEmpty() ? d.title : d.title + "\n§8" + date);
			actions.add(() -> open(player, d, CONTENTS));
		}
		if (!updates.isEmpty()) {
			WhatsNewDoc latest = updates.latest();
			form.button("§2Latest: " + latest.title);
			actions.add(() -> open(player, latest, CONTENTS));
		}
		form.button("Close");
		actions.add(() -> plugin.markSeen(player));
		send(player, form, actions);
	}

	private void page(Player player, WhatsNewDoc doc, int index) {
		WhatsNewDoc.Page page = doc.pages.get(index);
		boolean last = index == doc.pages.size() - 1;
		String body = Render.legacy(page.lines()) + "\n\n§8Page " + (index + 1) + " of " + doc.pages.size();

		List<Runnable> actions = new ArrayList<>();
		boolean isNew = plugin.newPages(player, doc).contains(index);
		SimpleForm.Builder form = SimpleForm.builder().title(isNew ? "New: " + page.title() : page.title()).content(body);
		if (!last) {
			form.button("§2Next: " + doc.pages.get(index + 1).title());
			actions.add(() -> open(player, doc, index + 1));
		}
		form.button("Contents");
		actions.add(() -> open(player, doc, CONTENTS));
		form.button(last ? "Done" : "Close");
		actions.add(() -> plugin.markSeen(player));
		send(player, form, actions);
	}

	private void send(Player player, SimpleForm.Builder form, List<Runnable> actions) {
		// Floodgate calls back off the main thread
		form.validResultHandler(response -> {
			int id = response.clickedButtonId();
			if (id >= 0 && id < actions.size()) Bukkit.getScheduler().runTask(plugin, actions.get(id));
		});
		// Closed with X counts as read; a form that failed to show doesn't
		form.closedResultHandler(() -> Bukkit.getScheduler().runTask(plugin, () -> plugin.markSeen(player)));
		FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
	}
}
