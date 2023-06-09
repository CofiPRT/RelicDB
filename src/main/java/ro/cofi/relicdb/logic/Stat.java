package ro.cofi.relicdb.logic;

import java.util.Arrays;
import java.util.List;

public enum Stat {
    SPD("SPD", "Speed"),
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

    public static final List<Stat> HEAD = List.of(
        HP
    );
    public static final List<Stat> HANDS = List.of(
        ATK
    );
    public static final List<Stat> BODY = List.of(
        HP_PCT, ATK_PCT, DEF_PCT, EFFECT_HIT_RATE, OUTGOING_HEALING_BOOST, CRIT_RATE, CRIT_DMG
    );
    public static final List<Stat> FEET = List.of(
        SPD, HP_PCT, ATK_PCT, DEF_PCT
    );
    public static final List<Stat> PLANAR_SPHERE = List.of(
        HP_PCT, ATK_PCT, DEF_PCT, PHYSICAL_DMG, FIRE_DMG, ICE_DMG, WIND_DMG, LIGHTNING_DMG, QUANTUM_DMG, IMAGINARY_DMG
    );
    public static final List<Stat> LINK_ROPE = List.of(
        HP_PCT, ATK_PCT, DEF_PCT, BREAK_EFFECT, ENERGY_REGEN_RATE
    );

    private final String[] names;

    Stat(String... name) {
        this.names = name;
    }

    public boolean matches(String target) {
        return Arrays.stream(names).anyMatch(target::equalsIgnoreCase);
    }

    @Override
    public String toString() {
        return names[0];
    }

    public static Stat[] valuesWithNull() {
        return Arrays.copyOf(values(), values().length + 1);
    }
}
