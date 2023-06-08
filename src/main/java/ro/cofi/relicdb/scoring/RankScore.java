package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.List;

public record RankScore(RankScoreType type, String otherSet) {

    public String getHTML() {
        String header = HTMLUtil.prependIcon(type.getIcon(), "Set -", type.getDescription());

        String additionalInfo = otherSet == null
            ? "To be used in a full set."
            : String.format("To be used with <i>%s</i>.", otherSet);

        return HTMLUtil.wrapInLineBreaks(List.of(header, HTMLUtil.wrapInList(List.of(additionalInfo))));
    }

}
