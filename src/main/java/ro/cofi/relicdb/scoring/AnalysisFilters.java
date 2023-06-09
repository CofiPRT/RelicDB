package ro.cofi.relicdb.scoring;

import java.util.Set;

public record AnalysisFilters(
    Set<RankScoreType> acceptedRankScores, Set<MainStatScoreType> acceptMainStatScores, int subStatScoreFilter
) { }
