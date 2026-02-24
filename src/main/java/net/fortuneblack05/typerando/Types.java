package net.fortuneblack05.typerando;

import net.minecraft.util.Identifier;
import java.util.Arrays;
import java.util.Optional;

public enum Types {
    NORMAL(12, "Normal", "\uE001"),
    FIRE(13, "Fire", "\uE002"),
    WATER(14, "Water", "\uE003"),
    ELECTRIC(15, "Electric", "\uE004"),
    GRASS(16, "Grass", "\uE005"),
    ICE(17, "Ice", "\uE006"),
    FIGHTING(18, "Fighting", "\uE007"),
    POISON(1, "Poison", "\uE008"),
    GROUND(2, "Ground", "\uE009"),
    FLYING(3, "Flying", "\uE00A"),
    PSYCHIC(4, "Psychic", "\uE00B"),
    BUG(5, "Bug", "\uE00C"),
    ROCK(6, "Rock", "\uE00D"),
    GHOST(7, "Ghost", "\uE00E"),
    DRAGON(8, "Dragon", "\uE00F"),
    DARK(9, "Dark", "\uE010"),
    STEEL(10, "Steel", "\uE011"),
    FAIRY(11, "Fairy", "\uE012");

    public final int id;
    public final String displayName;
    public final Identifier texture;
    public final String tabIcon; // <-- ADD THIS

    Types(int id, String displayName, String tabIcon) {
        this.id = id;
        this.displayName = displayName;
        this.texture = Identifier.of("type-picker", "textures/gui/types/" + id + ".png");
        this.tabIcon = tabIcon; // <-- ADD THIS
    }

    public static Optional<Types> fromId(int id) {
        return Arrays.stream(values()).filter(t -> t.id == id).findFirst();
    }
}