package com.dabomstew.pkrandom.log;

import java.util.List;
import java.util.Map;

/**
 * Formats the manual edit registry contents into a plain-text report that can
 * be displayed or saved by the editor.
 */
public final class ManualEditLogFormatter {

    private ManualEditLogFormatter() {
        // utility class
    }

    public static String buildLogText(Map<String, List<String>> sections) {
        if (sections == null || sections.isEmpty()) {
            return "No manual edits recorded.";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            String section = entry.getKey();
            if (section == null || section.isEmpty()) {
                section = "General";
            }
            sb.append("=== ").append(section).append(" ===\n");
            List<String> lines = entry.getValue();
            if (lines == null || lines.isEmpty()) {
                sb.append("(no details)\n");
            } else {
                for (String line : lines) {
                    sb.append(" - ").append(line).append('\n');
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
