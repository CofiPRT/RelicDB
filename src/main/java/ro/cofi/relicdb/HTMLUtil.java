package ro.cofi.relicdb;

import java.util.List;
import java.util.stream.Collectors;

public class HTMLUtil {
    public static final String EMOJI_TICK = "✔";
    public static final String EMOJI_DIAMOND = "🔸";
    public static final String EMOJI_CROSS = "❌";

    public static String prependIcon(String icon, String... text) {
        return String.format("%s %s", icon, String.join(" ", text));
    }

    public static String wrapInList(List<String> items) {
        return String.format(
            "<ul>%s</ul>",
            items.stream().map(item -> String.format("<li>%s</li>", item)).collect(Collectors.joining())
        );
    }

    public static String wrapInLineBreaks(List<String> items) {
        return String.join("<br>", items);
    }
}
