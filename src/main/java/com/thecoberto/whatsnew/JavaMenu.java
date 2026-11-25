package com.thecoberto.whatsnew;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.thecoberto.whatsnew.WhatsNewPlugin.CONTENTS;
import static com.thecoberto.whatsnew.WhatsNewPlugin.PAST;

/**
 * The menu for Java players: a dialog (the pop-up screen Minecraft 1.21.6+ has). An
 * update's contents screen lists its pages; each page has Next, Contents and Close. The
 * current update's contents has Past Updates, which lists older updates to read the same
 * way. It can't be closed with Esc, so pressing Close (or Done) is what counts as having read it.
 */
final class JavaMenu {
	private static final int TEXT_WIDTH = 320;
	private static final int BUTTON_WIDTH = 220;

	private final WhatsNewPlugin plugin;

	JavaMenu(WhatsNewPlugin plugin) {
		this.plugin = plugin;
	}

	void open(Player player, WhatsNewDoc doc, int screen) {
		Updates updates = plugin.updates();
		Dialog d;
		if (screen == PAST) d = past(player, updates);
		else if (screen < 0 || screen >= doc.pages.size()) d = contents(player, updates, doc);
		else d = page(player, updates, doc, screen);
		player.showDialog(d);
	}

	private Dialog contents(Player player, Updates updates, WhatsNewDoc doc) {
		boolean latest = updates.isLatest(doc);
		List<DialogBody> body = new ArrayList<>();
		String date = updates.date(doc);
		if (!latest && !date.isEmpty()) body.add(DialogBody.plainMessage(Component.text(date, NamedTextColor.DARK_GRAY), TEXT_WIDTH));
		if (Render.hasContent(doc.intro)) body.add(DialogBody.plainMessage(Render.lines(doc.intro), TEXT_WIDTH));
		if (!doc.pages.isEmpty()) {
			body.add(DialogBody.plainMessage(Component.text("Pick a section, or start from the top.", NamedTextColor.GRAY), TEXT_WIDTH));
		}

		List<ActionButton> buttons = new ArrayList<>();
		if (!doc.pages.isEmpty()) {
			buttons.add(button(Component.text("Start reading ▶", NamedTextColor.GREEN), () -> open(player, doc, 0)));
			for (int i = 0; i < doc.pages.size(); i++) {
				int page = i;
				buttons.add(button(Component.text(doc.pages.get(i).title()), () -> open(player, doc, page)));
			}
		}
		if (latest && !updates.past(plugin.historyLimit()).isEmpty()) {
			buttons.add(button(Component.text("Past Updates ▶", NamedTextColor.AQUA), () -> open(player, doc, PAST)));
		} else if (!latest) {
			buttons.add(button(Component.text("◀ Past Updates", NamedTextColor.AQUA), () -> open(player, doc, PAST)));
		}
		return dialog(doc.title, body, buttons, close(player, "Close"));
	}

	private Dialog past(Player player, Updates updates) {
		List<DialogBody> body = List.of(DialogBody.plainMessage(
				Component.text("Pick an update to read what it brought.", NamedTextColor.GRAY), TEXT_WIDTH));
		List<ActionButton> buttons = new ArrayList<>();
		for (WhatsNewDoc d : updates.past(plugin.historyLimit())) {
			String date = updates.date(d);
			Component label = Component.text(d.title);
			if (!date.isEmpty()) label = label.append(Component.text(" (" + date + ")", NamedTextColor.GRAY));
			buttons.add(button(label, () -> open(player, d, CONTENTS)));
		}
		if (!updates.isEmpty()) {
			WhatsNewDoc latest = updates.latest();
			buttons.add(button(Component.text("◀ Latest: " + latest.title, NamedTextColor.GREEN), () -> open(player, latest, CONTENTS)));
		}
		return dialog("Past Updates", body, buttons, close(player, "Close"));
	}

	private Dialog page(Player player, Updates updates, WhatsNewDoc doc, int index) {
		WhatsNewDoc.Page page = doc.pages.get(index);
		boolean last = index == doc.pages.size() - 1;

		List<DialogBody> body = new ArrayList<>();
		body.add(DialogBody.plainMessage(
				Component.text(page.title(), NamedTextColor.YELLOW).decorate(TextDecoration.BOLD), TEXT_WIDTH));
		if (Render.hasContent(page.lines())) body.add(DialogBody.plainMessage(Render.lines(page.lines()), TEXT_WIDTH));
		body.add(DialogBody.plainMessage(
				Component.text("Page " + (index + 1) + " of " + doc.pages.size(), NamedTextColor.DARK_GRAY), TEXT_WIDTH));

		List<ActionButton> buttons = new ArrayList<>();
		if (!last) {
			String next = doc.pages.get(index + 1).title();
			buttons.add(button(Component.text("Next: " + next + " ▶", NamedTextColor.GREEN), () -> open(player, doc, index + 1)));
		}
		buttons.add(button(Component.text("◀ Contents"), () -> open(player, doc, CONTENTS)));
		return dialog(doc.title, body, buttons, close(player, last ? "Done" : "Close"));
	}

	private ActionButton close(Player player, String label) {
		return button(Component.text(label), () -> plugin.markSeen(player));
	}

	private static Dialog dialog(String title, List<DialogBody> body, List<ActionButton> buttons, ActionButton exit) {
		DialogBase base = DialogBase.builder(Component.text(title, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
				.canCloseWithEscape(false)
				.afterAction(DialogBase.DialogAfterAction.CLOSE)
				.body(body)
				.build();
		DialogType type = buttons.isEmpty()
				? DialogType.notice(exit)
				: DialogType.multiAction(buttons, exit, 1);
		return Dialog.create(b -> b.empty().base(base).type(type));
	}

	private ActionButton button(Component label, Runnable onClick) {
		ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofHours(1)).build();
		return ActionButton.builder(label)
				.width(BUTTON_WIDTH)
				.action(DialogAction.customClick((view, audience) -> Bukkit.getScheduler().runTask(plugin, onClick), once))
				.build();
	}
}
