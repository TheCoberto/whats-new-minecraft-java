package com.thecoberto.whatsnew;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * whats-new.md, read into pages. The format (the Bedrock add-on reads the same one):
 *
 * <pre>
 * # Title of the update                 menu title
 * update: 2026-11-24                    changing this shows the menu to everyone again
 *
 * Intro text, shown on the contents page.
 *
 * ## Page title                          each ## starts a page
 * ### Small heading
 * - bullet, with **bold** and `commands`
 * > a grey note
 * &lt;!-- comments are left out --&gt;
 * </pre>
 */
public final class WhatsNewDoc {
	public enum Kind { TEXT, BULLET, HEADING, NOTE, BLANK }

	/** A piece of a line. */
	public record Span(String text, boolean bold, boolean code) {}

	public record Line(Kind kind, List<Span> spans) {
		public String plain() {
			StringBuilder sb = new StringBuilder();
			for (Span s : spans) sb.append(s.text());
			return sb.toString();
		}
	}

	public record Page(String title, List<Line> lines) {}

	private static final Pattern UPDATE = Pattern.compile("^(?i)update\\s*:\\s*(.+)$");
	private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)]\\([^)]*\\)");

	public final String title;
	/** Changes whenever there's a new update. Empty when the file has nothing in it. */
	public final String update;
	public final List<Line> intro;
	public final List<Page> pages;

	private WhatsNewDoc(String title, String update, List<Line> intro, List<Page> pages) {
		this.title = title;
		this.update = update;
		this.intro = intro;
		this.pages = pages;
	}

	public boolean isEmpty() {
		return update.isEmpty() || (pages.isEmpty() && intro.isEmpty());
	}

	public static WhatsNewDoc parse(String markdown) {
		String text = markdown.replace("\r", "").replaceAll("(?s)<!--.*?-->", "");
		String title = "What's New";
		String update = "";
		List<Line> intro = new ArrayList<>();
		List<Page> pages = new ArrayList<>();
		List<Line> current = intro;
		boolean sawTitle = false;

		for (String raw : text.split("\n", -1)) {
			String line = raw.strip();
			if (line.startsWith("## ") && !line.startsWith("### ")) {
				current = new ArrayList<>();
				pages.add(new Page(line.substring(3).strip(), current));
				continue;
			}
			if (line.startsWith("# ") && !sawTitle) {
				title = line.substring(2).strip();
				sawTitle = true;
				continue;
			}
			Matcher m = UPDATE.matcher(line);
			if (m.matches() && pages.isEmpty() && update.isEmpty()) {
				update = m.group(1).strip();
				continue;
			}
			if (line.isEmpty()) {
				// One blank line between blocks, none at the start
				if (!current.isEmpty() && current.get(current.size() - 1).kind() != Kind.BLANK) {
					current.add(new Line(Kind.BLANK, List.of()));
				}
			} else if (line.startsWith("### ")) {
				current.add(new Line(Kind.HEADING, spans(line.substring(4).strip())));
			} else if (line.startsWith("- ") || line.startsWith("* ")) {
				current.add(new Line(Kind.BULLET, spans(line.substring(2).strip())));
			} else if (line.startsWith(">")) {
				current.add(new Line(Kind.NOTE, spans(line.substring(1).strip())));
			} else {
				current.add(new Line(Kind.TEXT, spans(line)));
			}
		}

		trim(intro);
		for (Page p : pages) trim(p.lines());
		// No update line: use the title, so a new title still counts as a new update
		if (update.isEmpty() && !(pages.isEmpty() && intro.isEmpty())) update = title;
		return new WhatsNewDoc(title, update, intro, pages);
	}

	private static void trim(List<Line> lines) {
		while (!lines.isEmpty() && lines.get(lines.size() - 1).kind() == Kind.BLANK) lines.remove(lines.size() - 1);
	}

	/** **bold**, `code` and [links](url) (shown as their text). */
	static List<Span> spans(String s) {
		List<Span> out = new ArrayList<>();
		Matcher m = INLINE.matcher(s);
		int at = 0;
		while (m.find()) {
			if (m.start() > at) out.add(new Span(s.substring(at, m.start()), false, false));
			if (m.group(1) != null) out.add(new Span(m.group(1), true, false));
			else if (m.group(2) != null) out.add(new Span(m.group(2), false, true));
			else out.add(new Span(m.group(3), false, false));
			at = m.end();
		}
		if (at < s.length()) out.add(new Span(s.substring(at), false, false));
		return out;
	}
}
