package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum MainStatScoreType {
    IDEAL(
        25, HTMLUtil.EMOJI_TICK, "IDEAL",
        "The best main stat for this character."
    ),
    UNACCEPTABLE(
        0, HTMLUtil.EMOJI_CROSS, "UNACCEPTABLE",
        "This main stat is not good for this character."
    );

    private final int score;
    private final String icon;
    private final String rankTitle;
    private final String description;

    MainStatScoreType(int score, String icon, String rankTitle, String description) {
        this.score = score;
        this.icon = icon;
        this.rankTitle = rankTitle;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public Set<MainStatScoreType> filteredBy(MainStatScoreType minimumScore) {
        return Arrays.stream(MainStatScoreType.values())
            .filter(type -> type.score >= minimumScore.score)
            .collect(Collectors.toUnmodifiableSet());
    }

    public Set<MainStatScoreType> getHigherScores() {
        return filteredBy(this);
    }
}
