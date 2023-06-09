package ro.cofi.relicdb.logic;

import java.util.List;

public enum RelicType {
    RELIC("Relic", "relics", RelicPart.RELICS),
    ORNAMENT("Planar Ornament", "ornaments", RelicPart.ORNAMENTS);

    private final String name;
    private final String jsonKey;
    private final List<RelicPart> parts;

    RelicType(String name, String jsonKey, List<RelicPart> parts) {
        this.name = name;
        this.jsonKey = jsonKey;
        this.parts = parts;
    }

    public String getJsonKey() {
        return jsonKey;
    }

    public List<RelicPart> getParts() {
        return parts;
    }

    @Override
    public String toString() {
        return name;
    }
}
