package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.HTMLUtil;

import java.util.ArrayList;
import java.util.List;

public class Score {

    private static final int MAX_SCORE_DISPLAY = 10;
    private static final int MAX_SCORE = MainStatScoreType.IDEAL.getScore() +
                                         RankScoreType.IDEAL.getScore() +
                                         computeSubStatScore(4);

    private final int totalScore;
    private final String character;
    private final String description;

    public Score(
        String character, String characterURL,
        RankScore rankScore, MainStatScore mainStatScore, List<SubStatScore> subStatScores
    ) {
        this.character = character;

        this.totalScore = rankScore.type().getScore() +
                          (mainStatScore != null
                              ? mainStatScore.type().getScore()
                              : MainStatScoreType.IDEAL.getScore()) +
                          computeSubStatScore(subStatScores);

        String header = String.format(
            "<b><a href=\"%s\">%s</a></b>: Score = %s",
            characterURL, character, scoreDisplay(totalScore)
        );
        String setDescription = rankScore.getHTML();
        String mainStatDescription = mainStatScore != null ? mainStatScore.getHTML() : null;

        String subStatsHeader = "Substats:";
        List<String> subStatDescriptions = subStatScores.stream().map(SubStatScore::getHTML).toList();

        String subStatsDescription = HTMLUtil.wrapInLineBreaks(
            List.of(subStatsHeader, HTMLUtil.wrapInList(subStatDescriptions))
        );

        List<String> descriptionParts = new ArrayList<>();
        descriptionParts.add(setDescription);
        if (mainStatDescription != null)
            descriptionParts.add(mainStatDescription);
        descriptionParts.add(subStatsDescription);

        this.description = HTMLUtil.wrapInLineBreaks(List.of(header, HTMLUtil.wrapInList(descriptionParts)));
    }

    private static int computeSubStatScore(List<SubStatScore> subStatScores) {
        int count = (int) subStatScores.stream()
            .filter(subStatScore -> subStatScore.type() == SubStatScoreType.MET)
            .count();

        return computeSubStatScore(count);
    }

    private static int computeSubStatScore(int count) {
        return SubStatScoreType.MET.getScore() * count * (count >= 2 ? 5 : 1);
    }

    private static String scoreDisplay(int score) {
        // receive a score between [0, MAX_SCORE]
        // shift it to a double between [0, MAX_SCORE_DISPLAY]
        double shiftedScore = (double) score / MAX_SCORE * MAX_SCORE_DISPLAY;

        // 1 decimal place if less than MAX_SCORE_DISPLAY
        if (shiftedScore < MAX_SCORE_DISPLAY)
            return String.format("%.1f/%d", shiftedScore, MAX_SCORE_DISPLAY);

        return String.format("%d/%d", MAX_SCORE_DISPLAY, MAX_SCORE_DISPLAY);
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
