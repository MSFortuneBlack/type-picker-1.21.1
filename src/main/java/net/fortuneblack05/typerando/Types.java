package net.fortuneblack05.typerando;

import java.util.Arrays;
import java.util.Optional;

public enum Types {
    NORMAL(1, "Normal"),
    FIRE(2, "Fire"),
    WATER(3, "Water"),
    ELECTRIC(4, "Electric"),
    GRASS(5, "Grass"),
    ICE(6, "Ice"),
    FIGHTING(7, "Fighting"),
    POISON(8, "Poison"),
    GROUND(9, "Ground"),
    FLYING(10, "Flying"),
    PSYCHIC(11, "Psychic"),
    BUG(12, "Bug"),
    ROCK(13, "Rock"),
    GHOST(14, "Ghost"),
    DRAGON(15, "Dragon"),
    DARK(16, "Dark"),
    STEEL(17, "Steel"),
    FAIRY(18, "Fairy");

    public final int id;              // The number (1..18)
    public final String displayName;  // The name you show to players

    Types(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    // Convert a number back into a Type
    public static Optional<Types> fromId(int id) {
        return Arrays.stream(values()).filter(t -> t.id == id).findFirst();
    }
}