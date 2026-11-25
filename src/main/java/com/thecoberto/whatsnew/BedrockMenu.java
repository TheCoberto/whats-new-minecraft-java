package com.thecoberto.whatsnew;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu for Bedrock players joining through Geyser: a Bedrock form sent with Floodgate.
 * Only loaded when Floodgate is installed. Same screens as the Java menu; closing the
 * form (X) or pressing Close/Done counts as having read it.
 */
final class BedrockMenu {
	private final WhatsNewPlugin plugin;

	BedrockMenu(WhatsNewPlugin plugin) {
		this.plugin = plugin;
	}

	boolean isBedrock(Player player) {
		return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
	}

	/** screen -1 is the contents, 0.. are pages. */
	void open(Player player, WhatsNewDoc doc, int screen) {
		if (screen < 0 || screen >= doc.pages.size()) contents(player, doc);
		else page(player, doc, screen);
	}

	private void contents(Player player, WhatsNewDoc doc) {
		StringBuilder body = new StringBuilder(Render.legacy(doc.intro));
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
			for (int i = 0; i < doc.pages.size(); i++) {
				int page = i;
				form.button(doc.pages.get(i).title());
				actions.add(() -> open(player, doc, page));
			}
		}
		form.button("Close");
		actions.add(() -> plugin.markSeen(player, doc));
		send(player, doc, form, actions);
	}

	private void page(Player player, WhatsNewDoc doc, int index) {
		WhatsNewDoc.Page page = doc.pages.get(index);
		boolean last = index == doc.pages.size() - 1;
		String body = Render.legacy(page.lines()) + "\n\n§8Page " + (index + 1) + " of " + doc.pages.size();

		List<Runnable> actions = new ArrayList<>();
		SimpleForm.Builder form = SimpleForm.builder().title(page.title()).content(body);
		if (!last) {
			form.button("§2Next: " + doc.pages.get(index + 1).title());
			actions.add(() -> open(player, doc, index + 1));
		}
		form.button("Contents");
		actions.add(() -> open(player, doc, -1));
		form.button(last ? "Done" : "Close");
		actions.add(() -> plugin.markSeen(player, doc));
		send(player, doc, form, actions);
	}

	private void send(Player player, WhatsNewDoc doc, SimpleForm.Builder form, List<Runnable> actions) {
		// Floodgate calls back off the main thread
		form.validResultHandler(response -> {
			int id = response.clickedButtonId();
			if (id >= 0 && id < actions.size()) Bukkit.getScheduler().runTask(plugin, actions.get(id));
		});
		// Closed with X counts as read; a form that failed to show doesn't
		form.closedResultHandler(() -> Bukkit.getScheduler().runTask(plugin, () -> plugin.markSeen(player, doc)));
		FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
	}
}
