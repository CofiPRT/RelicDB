package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.List;

public record RankScore(RankScoreType type, String otherSet) {

    public String getHTML() {
        String header = String.format(
            "%s Set - <b>%s</b>: %s",
            type.getIcon(),
            type.getRankTitle(),
            otherSet == null
                ? type.getFullSetDescription()
                : String.format(type.getDuoSetDescription(), otherSet)
        );

        return HTMLUtil.wrapInLineBreaks(List.of(header));
    }

}
