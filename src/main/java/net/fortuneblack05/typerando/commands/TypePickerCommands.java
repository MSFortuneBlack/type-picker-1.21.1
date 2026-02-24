package net.fortuneblack05.typerando.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fortuneblack05.typerando.TypePicker;
import net.fortuneblack05.typerando.Types;
import net.fortuneblack05.typerando.payloads.SpinResult;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TypePickerCommands {
    private static final Random RNG = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("typepicker")
                // OP-only: permission level 2+ (you can change to 4 if you want stricter)
                .requires(src -> src.hasPermissionLevel(2))

                // /typepicker  -> assign to everyone online who doesn't have a role yet
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    List<ServerPlayerEntity> players = src.getServer().getPlayerManager().getPlayerList();

                    int assignedCount = 0;

                    for (ServerPlayerEntity p : players) {
                        if (TypePicker.MANAGER.hasRole(p.getUuid())) continue;

                        Optional<Types> assigned = TypePicker.MANAGER.assignIfMissing(p.getUuid());
                        if (assigned.isEmpty()) {
                            // no roles left; stop early
                            src.sendError(Text.literal("No types left (all 18 are taken)."));
                            break;
                        }

                        // Save after each assignment to be safe
                        TypePicker.MANAGER.save(src.getServer());

                        // Tell THIS player to show the wheel UI
                        int spinTicks = 80 + RNG.nextInt(60); // ~4-7 seconds at 20tps
                        ServerPlayNetworking.send(p, new SpinResult(assigned.get().id, spinTicks));

                        assignedCount++;
                    }

                    final int assignedCountFinal = assignedCount;

                    src.sendFeedback(() -> Text.literal("TypePicker: assigned " + assignedCountFinal + " player(s)."), true);
                    return 1;
                })


                        // ... inside your dispatcher.register(literal("typepicker") block ...

// /typepicker clear <player>
                        .then(literal("clear")
                                .then(argument("player", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            String name = StringArgumentType.getString(ctx, "player");
                                            ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);

                                            if (target == null) {
                                                src.sendError(Text.literal("Player must be online."));
                                                return 0;
                                            }

                                            TypePicker.MANAGER.clearRole(target.getUuid());
                                            TypePicker.MANAGER.save(src.getServer());
                                            src.sendFeedback(() -> Text.literal("Cleared type for " + target.getName().getString()), true);
                                            return 1;
                                        })
                                )
                        )

                    // /typepicker reroll <player>
                        .then(literal("reroll")
                                .then(argument("player", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerCommandSource src = ctx.getSource();
                                            String name = StringArgumentType.getString(ctx, "player");
                                            ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);

                                            if (target == null) {
                                                src.sendError(Text.literal("Player must be online."));
                                                return 0;
                                            }

                                            // Clear their current role first
                                            TypePicker.MANAGER.clearRole(target.getUuid());

                                            // Assign a new one based on weights/blacklists
                                            Optional<Types> assigned = TypePicker.MANAGER.assignIfMissing(target.getUuid());
                                            if (assigned.isEmpty()) {
                                                src.sendError(Text.literal("No valid types left to assign them!"));
                                                return 0;
                                            }

                                            TypePicker.MANAGER.save(src.getServer());

                                            // Trigger the spin animation
                                            int spinTicks = 80 + RNG.nextInt(60);
                                            ServerPlayNetworking.send(target, new SpinResult(assigned.get().id, spinTicks));

                                            src.sendFeedback(() -> Text.literal("Rerolled type for " + target.getName().getString()), true);
                                            return 1;
                                        })
                                )
                        )

                // /typepicker <playerName>
                .then(argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            String name = StringArgumentType.getString(ctx, "player");

                            ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);
                            if (target == null) {
                                src.sendError(Text.literal("Player not found (must be online): " + name));
                                return 0;
                            }

                            if (TypePicker.MANAGER.hasRole(target.getUuid())) {
                                src.sendError(Text.literal("That player already has a type."));
                                return 0;
                            }

                            Optional<Types> assigned = TypePicker.MANAGER.assignIfMissing(target.getUuid());
                            if (assigned.isEmpty()) {
                                src.sendError(Text.literal("No types left (all 18 are taken)."));
                                return 0;
                            }

                            TypePicker.MANAGER.save(src.getServer());

                            int spinTicks = 80 + RNG.nextInt(60);
                            ServerPlayNetworking.send(target, new SpinResult(assigned.get().id, spinTicks));

                            src.sendFeedback(() -> Text.literal("TypePicker: spun for " + target.getName().getString()), true);
                            return 1;
                        })
                )
        );
    }
}