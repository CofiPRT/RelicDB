package ro.cofi.relicdb.logic;

public enum RelicPart {
    HEAD("Head", null),
    HAND("Hands", null),
    BODY("Body", "body"),
    FEET("Feet", "feet"),
    PLANAR_SPHERE("Planar Sphere", "planarSphere"),
    LINK_ROPE("Link Rope", "linkRope");

    private final String name;
    private final String jsonKey;

    RelicPart(String name, String jsonKey) {
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
