package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

public record SubStatScore(SubStatScoreType type, String subStat) {

    public String getHTML() {
        return HTMLUtil.prependIcon(type.getIcon(), subStat);
    }

}
