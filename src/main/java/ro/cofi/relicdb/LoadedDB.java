package ro.cofi.relicdb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ro.cofi.relicdb.io.DBChoice;
import ro.cofi.relicdb.logic.Stat;
import ro.cofi.relicdb.scoring.AnalysisRecipe;
import ro.cofi.relicdb.scoring.MainStatScore;
import ro.cofi.relicdb.scoring.MainStatScoreType;
import ro.cofi.relicdb.scoring.RankScore;
import ro.cofi.relicdb.scoring.RankScoreType;
import ro.cofi.relicdb.scoring.Score;
import ro.cofi.relicdb.scoring.SubStatScore;
import ro.cofi.relicdb.scoring.SubStatScoreType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class LoadedDB {

    private final JsonObject data;
    private final DBChoice dbChoice;

    public LoadedDB(JsonObject data, DBChoice dbChoice) {
        this.data = data;
        this.dbChoice = dbChoice;
    }

    public JsonObject getData() {
        return data;
    }

    public DBChoice getDBChoice() {
        return dbChoice;
    }

    public String analyzeItem(AnalysisRecipe recipe) {
        List<Score> scores = getScores(recipe);

        // sort scores by score, then by character name
        scores.sort(Comparator.comparing(Score::getTotalScore).reversed().thenComparing(Score::getCharacter));

        return HTMLUtil.wrapInList(scores.stream().map(Score::getDescription).toList());
    }

    private List<Score> getScores(AnalysisRecipe recipe) {
        List<Score> scores = new ArrayList<>();

        JsonArray characters = data.getAsJsonArray("characters");
        List<Stat> ownedSubStats = List.of(
            recipe.subStat1(),
            recipe.subStat2(),
            recipe.subStat3(),
            recipe.subStat4()
        );

        for (int i = 0; i < characters.size(); i++) {
            JsonObject character = characters.get(i).getAsJsonObject();
            String characterName = character.get("name").getAsString();

            JsonArray weaponOptions = character.getAsJsonArray(recipe.type().getJsonKey());

            for (int j = 0; j < weaponOptions.size(); j++) {
                JsonObject weaponOption = weaponOptions.get(j).getAsJsonObject();

                JsonArray weaponSets = weaponOption.getAsJsonArray("sets");

                int weaponSetCount = weaponSets.size();
                int foundIndex;
                for (foundIndex = 0; foundIndex < weaponSetCount; foundIndex++) {
                    JsonObject weaponSet = weaponSets.get(foundIndex).getAsJsonObject();

                    String weaponSetName = weaponSet.get("name").getAsString();

                    if (weaponSetName.equals(recipe.name()))
                        break;
                }

                if (foundIndex == weaponSetCount)
                    continue;

                // we found a matching set, construct a score
                RankScore rankScore = getRankScore(weaponOption, weaponSets, weaponSetCount, foundIndex);
                MainStatScore mainStatScore = getMainStatScore(recipe, character);
                List<SubStatScore> subStatScores = getSubStatScores(ownedSubStats, character);

                Score score = new Score(
                    characterName, character.get("url").getAsString(),
                    rankScore, mainStatScore, subStatScores
                );

                scores.add(score);
            }
        }

        return scores;
    }

    private RankScore getRankScore(
        JsonObject weaponOption, JsonArray weaponSets, int weaponSetCount, int foundIndex
    ) {
        int rank = weaponOption.get("rank").getAsInt();
        RankScoreType rankScoreType = rank == 1 ? RankScoreType.IDEAL : RankScoreType.ACCEPTABLE;
        String otherSet = weaponSetCount == 1
            ? null
            : weaponSets.get(1 - foundIndex).getAsJsonObject().get("name").getAsString();

        RankScore rankScore = new RankScore(rankScoreType, otherSet);
        return rankScore;
    }

    private MainStatScore getMainStatScore(AnalysisRecipe recipe, JsonObject character) {
        int foundIndex;
        MainStatScore mainStatScore = null; // some relics have fixed main stats

        String jsonKey = recipe.part().getJsonKey();
        if (jsonKey != null) {
            JsonObject mainStatsObject = character.getAsJsonObject("mainStats");
            String mainStatOptionsRaw = mainStatsObject.get(jsonKey).getAsString();
            String[] mainStatOptions = mainStatOptionsRaw.split(" / ");

            int mainStatCount = mainStatOptions.length;
            for (foundIndex = 0; foundIndex < mainStatCount; foundIndex++) {
                String mainStatOption = mainStatOptions[foundIndex];

                if (recipe.mainStat().matches(mainStatOption))
                    break;
            }

            MainStatScoreType mainStatScoreType = foundIndex == mainStatCount
                ? MainStatScoreType.UNACCEPTABLE
                : MainStatScoreType.IDEAL;

            String otherMainStat = null;
            if (mainStatScoreType == MainStatScoreType.UNACCEPTABLE)
                otherMainStat = mainStatOptionsRaw;
            else if (mainStatCount > 1)
                otherMainStat = mainStatOptions[1 - foundIndex];

            mainStatScore = new MainStatScore(mainStatScoreType, otherMainStat);
        }
        return mainStatScore;
    }

    private List<SubStatScore> getSubStatScores(List<Stat> ownedSubStats, JsonObject character) {
        JsonArray subStatsObject = character.getAsJsonArray("subStats");

        List<SubStatScore> subStatScores = new ArrayList<>();

        for (int k = 0; k < subStatsObject.size(); k++) {
            String subStatRaw = subStatsObject.get(k).getAsString();
            String[] subStats = subStatRaw.split(" / ");

            boolean match = ownedSubStats.stream().anyMatch(ownedSubStat ->
                Arrays.stream(subStats).anyMatch(ownedSubStat::matches)
            );

            SubStatScoreType subStatScoreType = match ? SubStatScoreType.MET : SubStatScoreType.UNMET;

            subStatScores.add(new SubStatScore(subStatScoreType, subStatRaw));
        }
        return subStatScores;
    }

}
