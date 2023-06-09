package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum RankScoreType {
    IDEAL(
        4, HTMLUtil.EMOJI_TICK, "IDEAL",
        "The best set for this character when used as a full set.",
        "The best set for this character when used with <i>%s</i>."
    ),
    ACCEPTABLE(
        2, HTMLUtil.EMOJI_PAWN, "ACCEPTABLE",
        "This set is okay when used as a full set.",
        "This set is okay when used with <i>%s</i>."
    ),
    UNACCEPTABLE(
        0, HTMLUtil.EMOJI_CROSS, "UNACCEPTABLE",
        "This set is not good for this character.",
        null
    );

    private final int score;
    private final String icon;
    private final String rankTitle;
    private final String fullSetDescription;
    private final String duoSetDescription;

    RankScoreType(int score, String icon, String rankTitle, String fullSetDescription, String duoSetDescription) {
        this.score = score;
        this.icon = icon;
        this.rankTitle = rankTitle;
        this.fullSetDescription = fullSetDescription;
        this.duoSetDescription = duoSetDescription == null ? fullSetDescription : duoSetDescription;
    }

    public int getScore() {
        return score;
    }

    public String getIcon() {
        return icon;
    }

    public String getRankTitle() {
        return rankTitle;
    }

    public String getFullSetDescription() {
        return fullSetDescription;
    }

    public String getDuoSetDescription() {
        return duoSetDescription;
    }

    public Set<RankScoreType> filteredBy(RankScoreType minimumScore) {
        return Arrays.stream(RankScoreType.values())
            .filter(type -> type.score >= minimumScore.score)
            .collect(Collectors.toUnmodifiableSet());
    }

    public Set<RankScoreType> getHigherScores() {
        return filteredBy(this);
    }
}
