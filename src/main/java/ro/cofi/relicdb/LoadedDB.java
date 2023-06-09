package ro.cofi.relicdb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ro.cofi.relicdb.io.DBChoice;
import ro.cofi.relicdb.logic.Stat;
import ro.cofi.relicdb.scoring.AnalysisFilters;
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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LoadedDB {

    private final JsonObject data;
    private final DBChoice dbChoice;

    public LoadedDB(JsonObject data, DBChoice dbChoice) {
        this.data = data;
        this.dbChoice = dbChoice;
    }

    public DBChoice getDBChoice() {
        return dbChoice;
    }

    public List<String> getWeaponNames(String jsonKey) {
        return data.getAsJsonArray(jsonKey).asList().stream().map(JsonElement::getAsString).toList();
    }

    public String analyzeItem(AnalysisRecipe recipe, AnalysisFilters filters) {
        List<Score> scores = getScores(recipe, filters);

        if (scores.isEmpty())
            return "No results found. Try applying less restrictive filters.";

        // sort scores by score, then by character name
        scores.sort(Comparator.comparing(Score::getTotalScore).reversed().thenComparing(Score::getCharacter));

        return HTMLUtil.wrapInLineBreaks(scores.stream().map(Score::getDescription).toList());
    }

    private List<Score> getScores(AnalysisRecipe recipe, AnalysisFilters filters) {
        List<Score> scores = new ArrayList<>();

        JsonArray characters = data.getAsJsonArray("characters");
        List<Stat> ownedSubStats = Stream.of(
            recipe.subStat1(),
            recipe.subStat2(),
            recipe.subStat3(),
            recipe.subStat4()
        ).filter(Objects::nonNull).toList();

        for (int i = 0; i < characters.size(); i++) {
            JsonObject character = characters.get(i).getAsJsonObject();

            MainStatScore mainStatScore = getMainStatScore(recipe, character);
            if (mainStatScore != null && !filters.acceptMainStatScores().contains(mainStatScore.type()))
                continue;

            List<SubStatScore> subStatScores = getSubStatScores(ownedSubStats, character);
            int metSubStats = (int) subStatScores.stream()
                .filter(subStatScore -> subStatScore.type() == SubStatScoreType.MET)
                .count();
            if (metSubStats < filters.subStatScoreFilter())
                continue;

            String characterName = character.get("name").getAsString();
            String characterURL = character.get("url").getAsString();
            JsonArray weaponOptions = character.getAsJsonArray(recipe.type().getJsonKey());

            boolean found = false;

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

                found = true;

                // we found a matching set, construct a score
                RankScore rankScore = getRankScore(weaponOption, weaponSets, weaponSetCount, foundIndex);
                if (!filters.acceptedRankScores().contains(rankScore.type()))
                    continue;

                Score score = new Score(
                    characterName, characterURL,
                    rankScore, mainStatScore, subStatScores
                );

                scores.add(score);
            }

            if (found || !filters.acceptedRankScores().contains(RankScoreType.UNACCEPTABLE))
                continue;

            // create a bad rank score
            RankScore rankScore = new RankScore(RankScoreType.UNACCEPTABLE, null);

            Score score = new Score(
                characterName, characterURL,
                rankScore, mainStatScore, subStatScores
            );

            scores.add(score);
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

        return new RankScore(rankScoreType, otherSet);
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
            if (mainStatScoreType == MainStatScoreType.UNACCEPTABLE) {
                otherMainStat = mainStatOptionsRaw;
            } else if (mainStatCount > 1) {
                // concatenate all options except the one we found
                otherMainStat = Arrays.stream(mainStatOptions)
                    .filter(mainStatOption -> !mainStatOption.equals(recipe.mainStat().toString()))
                    .collect(Collectors.joining(" / "));
            }

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
