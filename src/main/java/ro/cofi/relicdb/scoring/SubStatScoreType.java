package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

public enum SubStatScoreType {
    MET(1, HTMLUtil.EMOJI_TICK),
    UNMET(0, HTMLUtil.EMOJI_CROSS);

    private final int score;
    private final String icon;

    SubStatScoreType(int score, String icon) {
        this.score = score;
        this.icon = icon;
    }

    public int getScore() {
        return score;
    }

    public String getIcon() {
        return icon;
    }
}
