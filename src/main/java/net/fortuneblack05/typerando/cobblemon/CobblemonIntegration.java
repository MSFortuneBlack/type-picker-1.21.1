package net.fortuneblack05.typerando.cobblemon;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import kotlin.Unit;
import net.fortuneblack05.typerando.TypePicker;
import net.fortuneblack05.typerando.Types;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Optional;

public class CobblemonIntegration {

    public static void register() {

        // 1. Prevent throwing out the Pokemon into the world with 'R'
        CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.NORMAL, event -> {
            Pokemon pokemon = event.getPokemon();
            ServerPlayerEntity player = pokemon.getOwnerPlayer();

            if (player != null) {
                Optional<Types> assigned = TypePicker.MANAGER.getRole(player.getUuid());
                if (assigned.isPresent()) {
                    if (!isMatchingType(pokemon, assigned.get())) {
                        event.cancel();
                        player.sendMessage(Text.literal("§cYou are a " + assigned.get().displayName + " trainer! You cannot use " + pokemon.getSpecies().getName() + " because it never evolves into your type."), true);
                    }
                }
            }
            return Unit.INSTANCE;
        });

        // 2. Prevent entering battles with invalid Pokemon
        CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.NORMAL, event -> {
            for (ServerPlayerEntity player : event.getBattle().getPlayers()) {
                Optional<Types> assigned = TypePicker.MANAGER.getRole(player.getUuid());

                if (assigned.isPresent()) {
                    boolean hasInvalid = false;
                    for (Pokemon partyMon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
                        if (partyMon != null) {
                            boolean isEgg = partyMon.getSpecies().getName().toLowerCase().contains("egg");

                            if (!isEgg && !isMatchingType(partyMon, assigned.get())) {
                                hasInvalid = true;
                                break;
                            }
                        }
                    }

                    if (hasInvalid) {
                        event.cancel();
                        player.sendMessage(Text.literal("§cBattle Cancelled! You have a Pokemon in your party that doesn't match or evolve into the " + assigned.get().displayName + " type!"), false);
                    }
                }
            }
            return Unit.INSTANCE;
        });
    }

    // --- THE EVOLUTION SCANNER ---
    private static boolean isMatchingType(Pokemon pokemon, Types assignedType) {
        String allowed = assignedType.displayName.toLowerCase();

        // 1. Check the Pokemon's current active types
        if (checkTypes(pokemon.getForm(), allowed)) return true;

        // 2. Look into the future! Check all possible direct evolutions (Stage 1)
        for (Evolution evolution : pokemon.getForm().getEvolutions()) {

            // The API returns the name as a String (e.g., "steelix"), so we must look it up in the registry!
            String evoName = evolution.getResult().getSpecies();
            net.minecraft.util.Identifier evoId = evoName.contains(":") ?
                    net.minecraft.util.Identifier.of(evoName) :
                    net.minecraft.util.Identifier.of("cobblemon", evoName);

            Species evoSpecies = PokemonSpecies.INSTANCE.getByIdentifier(evoId);

            if (evoSpecies != null) {
                FormData evoForm = evoSpecies.getStandardForm();
                if (checkTypes(evoForm, allowed)) return true;

                // 3. Check Stage 2 evolutions (e.g. Charmander -> Charmeleon -> Charizard (Flying))
                for (Evolution stage2 : evoForm.getEvolutions()) {

                    String stage2Name = stage2.getResult().getSpecies();
                    net.minecraft.util.Identifier stage2Id = stage2Name.contains(":") ?
                            net.minecraft.util.Identifier.of(stage2Name) :
                            net.minecraft.util.Identifier.of("cobblemon", stage2Name);

                    Species stage2Species = PokemonSpecies.INSTANCE.getByIdentifier(stage2Id);

                    if (stage2Species != null) {
                        FormData stage2Form = stage2Species.getStandardForm();
                        if (checkTypes(stage2Form, allowed)) return true;
                    }
                }
            }
        }

        return false;
    }

    // Simple helper to keep the code clean
    private static boolean checkTypes(FormData form, String allowed) {
        String type1 = form.getPrimaryType().getName().toLowerCase();
        String type2 = form.getSecondaryType() != null ? form.getSecondaryType().getName().toLowerCase() : "";
        return type1.contains(allowed) || type2.contains(allowed);
    }
}