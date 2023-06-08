package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.ArrayList;
import java.util.List;

public class Score {
    private final int totalScore;
    private final String character;
    private final String description;

    public Score(
        String character, String characterURL,
        RankScore rankScore, MainStatScore mainStatScore, List<SubStatScore> subStatScores
    ) {
        this.character = character;

        this.totalScore = rankScore.type().getScore() +
                          (mainStatScore != null ? mainStatScore.type().getScore() : 0) +
                          subStatScores.stream().mapToInt(subStatScore -> subStatScore.type().getScore()).sum();

        String header = String.format("<b><a href=\"%s\">%s</a></b>: Score = %d", characterURL, character, totalScore);
        String setDescription = rankScore.getHTML();
        String mainStatDescription = mainStatScore != null ? mainStatScore.getHTML() : null;

        String subStatsHeader = "Substats:";
        List<String> subStatDescriptions = subStatScores.stream().map(SubStatScore::getHTML).toList();

        String subStatsDescription =
            HTMLUtil.wrapInLineBreaks(List.of(subStatsHeader, HTMLUtil.wrapInList(subStatDescriptions)));

        List<String> descriptionParts = new ArrayList<>();
        descriptionParts.add(setDescription);
        if (mainStatDescription != null)
            descriptionParts.add(mainStatDescription);
        descriptionParts.add(subStatsDescription);

        this.description = HTMLUtil.wrapInLineBreaks(List.of(header, HTMLUtil.wrapInList(descriptionParts)));
    }

    public int getTotalScore() {
        return totalScore;
    }

    public String getCharacter() {
        return character;
    }

    public String getDescription() {
        return description;
    }
}
