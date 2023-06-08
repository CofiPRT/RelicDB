package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

public enum MainStatScoreType {
    IDEAL(
        10, HTMLUtil.EMOJI_TICK,
        "<b>IDEAL</b>: The best main stat for this character."
    ),
    UNACCEPTABLE(
        0, HTMLUtil.EMOJI_CROSS,
        "<b>UNACCEPTABLE</b>: This main stat is not good for this character."
    );

    private final int score;
    private final String icon;
    private final String description;

    MainStatScoreType(int score, String icon, String description) {
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
