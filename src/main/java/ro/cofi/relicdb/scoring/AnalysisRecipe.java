package ro.cofi.relicdb.scoring;

import ro.cofi.relicdb.logic.RelicPart;
import ro.cofi.relicdb.logic.RelicType;
import ro.cofi.relicdb.logic.Stat;

public record AnalysisRecipe(
    RelicType type, RelicPart part,
    String name, Stat mainStat,
    Stat subStat1, Stat subStat2, Stat subStat3, Stat subStat4
) { }
