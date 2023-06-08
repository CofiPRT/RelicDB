package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.ArrayList;
import java.util.List;

public record MainStatScore(MainStatScoreType type, String extraInfo) {

    public String getHTML() {
        String header = HTMLUtil.prependIcon(type.getIcon(), "Main Stat -", type.getDescription());

        List<String> details = new ArrayList<>();

        if (type == MainStatScoreType.UNACCEPTABLE)
            details.add(String.format("The preferred main stat is <b>%s</b>.", extraInfo));
        else if (extraInfo != null)
            details.add(String.format("Another good main stat is <b>%s</b>.", extraInfo));

        return HTMLUtil.wrapInLineBreaks(List.of(header, HTMLUtil.wrapInList(details)));
    }

}
