package ro.cofi.relicdb.logic;

import java.util.List;

public enum RelicPart {
    HEAD("Head", null, Stat.HEAD),
    HANDS("Hands", null, Stat.HANDS),
    BODY("Body", "body", Stat.BODY),
    FEET("Feet", "feet", Stat.FEET),
    PLANAR_SPHERE("Planar Sphere", "planarSphere", Stat.PLANAR_SPHERE),
    LINK_ROPE("Link Rope", "linkRope", Stat.LINK_ROPE);

    public static final List<RelicPart> RELICS = List.of(HEAD, HANDS, BODY, FEET);
    public static final List<RelicPart> ORNAMENTS = List.of(PLANAR_SPHERE, LINK_ROPE);

    private final String name;
    private final String jsonKey;
    private final List<Stat> availableStats;

    RelicPart(String name, String jsonKey, List<Stat> availableStats) {
        this.name = name;
        this.jsonKey = jsonKey;
        this.availableStats = availableStats;
    }

    public String getJsonKey() {
        return jsonKey;
    }

    public List<Stat> getAvailableStats() {
        return availableStats;
    }

    @Override
    public String toString() {
        return name;
    }
}
