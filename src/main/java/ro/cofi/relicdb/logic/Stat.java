package ro.cofi.relicdb.logic;

import java.util.Arrays;
import java.util.List;

public enum Stat {
    SPD("SPD"),
    HP("HP"),
    ATK("ATK"),
    HP_PCT("HP%"),
    ATK_PCT("ATK%"),
    DEF_PCT("DEF%"),
    BREAK_EFFECT("Break Effect"),
    EFFECT_HIT_RATE("Effect Hit Rate"),
    ENERGY_REGEN_RATE("Energy Regen Rate", "Energy Regeneration Rate"),
    OUTGOING_HEALING_BOOST("Outgoing Healing", "Outgoing Healing Boost", "Outgoing Heal Boost"),
    PHYSICAL_DMG("Physical DMG", "Physical DMG Boost"),
    FIRE_DMG("Fire DMG", "Fire DMG Boost"),
    ICE_DMG("Ice DMG", "Ice DMG Boost"),
    WIND_DMG("Wind DMG", "Wind DMG Boost"),
    LIGHTNING_DMG("Lightning DMG", "Lightning DMG Boost"),
    QUANTUM_DMG("Quantum DMG", "Quantum DMG Boost"),
    IMAGINARY_DMG("Imaginary DMG", "Imaginary DMG Boost"),
    CRIT_RATE("CRIT Rate"),
    CRIT_DMG("CRIT DMG");

    private static final List<Stat> HEAD = List.of(
        HP
    );
    private static final List<Stat> HANDS = List.of(
        ATK
    );
    private static final List<Stat> BODY = List.of(
        HP_PCT, ATK_PCT, DEF_PCT, EFFECT_HIT_RATE, OUTGOING_HEALING_BOOST, CRIT_RATE, CRIT_DMG
    );
    private static final List<Stat> FEET = List.of(
        SPD, HP_PCT, ATK_PCT, DEF_PCT
    );
    private static final List<Stat> PLANAR_SPHERE = List.of(
        HP_PCT, ATK_PCT, DEF_PCT, PHYSICAL_DMG, FIRE_DMG, ICE_DMG, WIND_DMG, LIGHTNING_DMG, QUANTUM_DMG, IMAGINARY_DMG
    );
    private static final List<Stat> LINK_ROPE = List.of(
        HP_PCT, ATK_PCT, DEF_PCT, BREAK_EFFECT, ENERGY_REGEN_RATE
    );

    private final String[] names;

    Stat(String... name) {
        this.names = name;
    }

    public String[] getNames() {
        return names;
    }

    public boolean matches(String target) {
        return Arrays.stream(names).anyMatch(target::equalsIgnoreCase);
    }

}
