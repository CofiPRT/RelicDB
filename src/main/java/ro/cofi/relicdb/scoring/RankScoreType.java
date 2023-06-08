package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

public enum RankScoreType {
    IDEAL(
        200, HTMLUtil.EMOJI_TICK,
        "<b>IDEAL</b>: The best set for this character."
    ),
    ACCEPTABLE(
        100, HTMLUtil.EMOJI_DIAMOND,
        "<b>ACCEPTABLE</b>: There are better sets for this character, but this will do."
    );

    private final int score;
    private final String icon;
    private final String description;

    RankScoreType(int score, String icon, String description) {
        this.score = score;
        this.icon = icon;
        this.description = description;
    }

    public int getScore() {
        return score;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

}
