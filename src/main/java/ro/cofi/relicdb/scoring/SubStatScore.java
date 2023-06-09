package ro.cofi.relicdb.scoring;

public record SubStatScore(SubStatScoreType type, String subStat) {

    public String getHTML() {
        return String.format("%s %s", type.getIcon(), subStat);
    }

}
