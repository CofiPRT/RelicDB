package ro.cofi.relicdb.logic;

public enum RelicType {
    RELIC("Relic", "relics"),
    ORNAMENT("Planar Ornament", "ornaments");

    private final String name;
    private final String jsonKey;

    RelicType(String name, String jsonKey) {
        this.name = name;
        this.jsonKey = jsonKey;
    }

    public String getName() {
        return name;
    }

    public String getJsonKey() {
        return jsonKey;
    }
}
