package com.thecoberto.whatsnew;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Every update's What's New, one file each in plugins/WhatsNew/updates/, newest first.
 * Files are sorted by name, so start each with its date: 2026-11-24-high-stakes.md.
 * The newest is the current update (it pops up on join); the rest are Past Updates.
 * A file's id is its "update:" line if it has one, otherwise its file name.
 *
 * Before 1.1.0 there was a single whats-new.md. It's still used when the folder has no files.
 */
final class Updates {
	private static final Pattern DATE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})");
	private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

	static final Updates EMPTY = new Updates(List.of(), Map.of());

	/** Newest first, empty files left out. */
	final List<WhatsNewDoc> all;
	private final Map<WhatsNewDoc, String> files;

	private Updates(List<WhatsNewDoc> all, Map<WhatsNewDoc, String> files) {
		this.all = all;
		this.files = files;
	}

	boolean isEmpty() {
		return all.isEmpty();
	}

	/** The current update. Only call when not empty. */
	WhatsNewDoc latest() {
		return all.get(0);
	}

	boolean isLatest(WhatsNewDoc doc) {
		return !all.isEmpty() && all.get(0) == doc;
	}

	/** Past updates, newest first, at most limit of them. */
	List<WhatsNewDoc> past(int limit) {
		return all.size() <= 1 ? List.of() : all.subList(1, Math.min(all.size(), 1 + Math.max(0, limit)));
	}

	/** "Nov 24, 2026" from a file name starting with a date, or "" without one. */
	String date(WhatsNewDoc doc) {
		Matcher m = DATE.matcher(files.getOrDefault(doc, ""));
		if (!m.find()) return "";
		int month = Integer.parseInt(m.group(2));
		if (month < 1 || month > 12) return "";
		return MONTHS[month - 1] + " " + Integer.parseInt(m.group(3)) + ", " + m.group(1);
	}

	/** Something that changes whenever a file is added, removed or edited. */
	static String signature(Path dir, Path legacy) {
		StringBuilder sb = new StringBuilder();
		try {
			for (Path p : markdown(dir)) sb.append(p.getFileName()).append(':').append(Files.getLastModifiedTime(p).toMillis()).append(';');
			if (Files.exists(legacy)) sb.append("legacy:").append(Files.getLastModifiedTime(legacy).toMillis());
		} catch (IOException e) {
			sb.append("error");
		}
		return sb.toString();
	}

	static Updates load(Path dir, Path legacy, Logger log) {
		List<Path> paths = markdown(dir);
		if (paths.isEmpty() && Files.exists(legacy)) paths = List.of(legacy);
		List<WhatsNewDoc> docs = new ArrayList<>();
		Map<WhatsNewDoc, String> names = new IdentityHashMap<>();
		for (Path p : paths) {
			String name = p.getFileName().toString().replaceFirst("(?i)\\.md$", "");
			try {
				WhatsNewDoc doc = WhatsNewDoc.parse(Files.readString(p, StandardCharsets.UTF_8), name);
				if (doc.isEmpty()) continue;
				docs.add(doc);
				names.put(doc, name);
			} catch (IOException e) {
				log.warning("Couldn't read " + p.getFileName() + ": " + e.getMessage());
			}
		}
		return new Updates(List.copyOf(docs), names);
	}

	/** The .md files in the folder, newest (by name) first. */
	private static List<Path> markdown(Path dir) {
		if (!Files.isDirectory(dir)) return List.of();
		try (Stream<Path> s = Files.list(dir)) {
			return s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase().endsWith(".md"))
					.sorted(Comparator.comparing((Path p) -> p.getFileName().toString().toLowerCase()).reversed())
					.toList();
		} catch (IOException e) {
			return List.of();
		}
	}
}
