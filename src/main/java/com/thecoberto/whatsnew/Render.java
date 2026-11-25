package com.thecoberto.whatsnew;

import com.thecoberto.whatsnew.WhatsNewDoc.Kind;
import com.thecoberto.whatsnew.WhatsNewDoc.Line;
import com.thecoberto.whatsnew.WhatsNewDoc.Span;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/** Turns whats-new.md lines into Java text components or Bedrock § text. */
final class Render {
	private Render() {}

	// ---------- Java ----------

	static Component lines(List<Line> lines) {
		TextComponent.Builder out = Component.text();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) out.append(Component.newline());
			out.append(line(lines.get(i)));
		}
		return out.build();
	}

	static Component line(Line line) {
		return switch (line.kind()) {
			case BLANK -> Component.empty();
			case HEADING -> spans(line.spans(), NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
			case BULLET -> Component.text()
					.append(Component.text("• ", NamedTextColor.GOLD))
					.append(spans(line.spans(), NamedTextColor.WHITE))
					.build();
			case NOTE -> spans(line.spans(), NamedTextColor.GRAY).decorate(TextDecoration.ITALIC);
			case TEXT -> spans(line.spans(), NamedTextColor.WHITE);
		};
	}

	private static Component spans(List<Span> spans, TextColor base) {
		TextComponent.Builder out = Component.text().color(base);
		for (Span s : spans) {
			Component c = Component.text(s.text());
			if (s.bold()) c = c.decorate(TextDecoration.BOLD).color(NamedTextColor.YELLOW);
			if (s.code()) c = c.color(NamedTextColor.AQUA);
			out.append(c);
		}
		return out.build();
	}

	// ---------- Bedrock (Floodgate forms) ----------

	static String legacy(List<Line> lines) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) out.append('\n');
			out.append(legacy(lines.get(i)));
		}
		return out.toString();
	}

	static String legacy(Line line) {
		return switch (line.kind()) {
			case BLANK -> "";
			case HEADING -> "§6§l" + plain(line) + "§r";
			case BULLET -> "§6• §r" + legacySpans(line.spans(), "§f");
			case NOTE -> "§7§o" + plain(line) + "§r";
			case TEXT -> legacySpans(line.spans(), "§f");
		};
	}

	private static String plain(Line line) {
		return line.plain().replace("§", "");
	}

	private static String legacySpans(List<Span> spans, String base) {
		StringBuilder out = new StringBuilder(base);
		for (Span s : spans) {
			String t = s.text().replace("§", "");
			if (s.bold()) out.append("§e§l").append(t).append("§r").append(base);
			else if (s.code()) out.append("§b").append(t).append(base);
			else out.append(t);
		}
		return out.append("§r").toString();
	}

	static boolean hasContent(List<Line> lines) {
		return lines.stream().anyMatch(l -> l.kind() != Kind.BLANK);
	}
}
