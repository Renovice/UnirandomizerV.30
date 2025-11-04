package com.dabomstew.pkrandom.log;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks manual edits performed through the editor UI so that they can be
 * presented to the user in a consolidated log.
 *
 * <p>The implementation intentionally mirrors the behaviour of the original
 * ZX project: edits are stored per-section in insertion order and callers
 * interact with the registry through a shared singleton.</p>
 */
public final class ManualEditRegistry {

    private static final ManualEditRegistry INSTANCE = new ManualEditRegistry();

    private final Map<String, List<String>> sections = new LinkedHashMap<>();

    private ManualEditRegistry() {
    }

    public static ManualEditRegistry getInstance() {
        return INSTANCE;
    }

    public synchronized void addEntry(String section, String entry) {
        if (section == null || section.isEmpty() || entry == null || entry.isEmpty()) {
            return;
        }
        sections.computeIfAbsent(section, key -> new ArrayList<>()).add(entry);
    }

    public synchronized void addEntries(String section, List<String> entries) {
        if (section == null || section.isEmpty() || entries == null || entries.isEmpty()) {
            return;
        }
        List<String> bucket = sections.computeIfAbsent(section, key -> new ArrayList<>());
        for (String entry : entries) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                bucket.add(trimmed);
            }
        }
    }

    public synchronized Map<String, List<String>> snapshot() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    public synchronized boolean hasEntries() {
        return !sections.isEmpty();
    }

    public synchronized void clear() {
        sections.clear();
    }
}
