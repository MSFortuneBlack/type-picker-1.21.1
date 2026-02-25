package net.fortuneblack05.typerando.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fortuneblack05.typerando.TypePicker;
import net.fortuneblack05.typerando.Types;
import net.fortuneblack05.typerando.payloads.SpinResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TypePickerCommands {
    private static final Random RNG = new Random();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("typepicker")
                .requires(src -> src.hasPermissionLevel(2))

                // 1. /typepicker (assign to everyone missing a type)
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    List<ServerPlayerEntity> players = src.getServer().getPlayerManager().getPlayerList();
                    int assignedCount = 0;

                    for (ServerPlayerEntity p : players) {
                        if (TypePicker.MANAGER.hasRole(p.getUuid())) continue;

                        Optional<Types> assigned = TypePicker.MANAGER.assignIfMissing(p.getUuid());
                        if (assigned.isEmpty()) {
                            src.sendError(Text.literal("No valid types left to assign."));
                            break;
                        }

                        TypePicker.MANAGER.save(src.getServer());

                        int spinTicks = 80 + RNG.nextInt(60);
                        // Tell the manager to hide the logo until the spin finishes
                        TypePicker.MANAGER.setSpinning(p.getUuid(), spinTicks);

                        ServerPlayNetworking.send(p, new SpinResult(assigned.get().id, spinTicks));
                        assignedCount++;
                    }

                    final int finalCount = assignedCount;
                    src.sendFeedback(() -> Text.literal("TypePicker: rolling for " + finalCount + " player(s)."), true);
                    return 1;
                })

                // 2. /typepicker clear <player>
                .then(literal("clear")
                        .then(argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    String name = StringArgumentType.getString(ctx, "player");
                                    ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);

                                    if (target == null) {
                                        src.sendError(Text.literal("Player not found: " + name));
                                        return 0;
                                    }

                                    TypePicker.MANAGER.clearRole(target.getUuid());
                                    TypePicker.MANAGER.save(src.getServer());

                                    // Clearing has no animation, so sync instantly!
                                    TypePicker.MANAGER.syncPlayerTab(target);

                                    src.sendFeedback(() -> Text.literal("Cleared type for " + target.getName().getString()), true);
                                    return 1;
                                })
                        )
                )

                // 3. /typepicker reroll <player>
                .then(literal("reroll")
                        .then(argument("player", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerCommandSource src = ctx.getSource();
                                    String name = StringArgumentType.getString(ctx, "player");
                                    ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);

                                    if (target == null) {
                                        src.sendError(Text.literal("Player not found: " + name));
                                        return 0;
                                    }

                                    TypePicker.MANAGER.clearRole(target.getUuid());
                                    Optional<Types> assigned = TypePicker.MANAGER.assignIfMissing(target.getUuid());

                                    if (assigned.isEmpty()) {
                                        src.sendError(Text.literal("No valid types left to assign!"));
                                        return 0;
                                    }

                                    TypePicker.MANAGER.save(src.getServer());

                                    int spinTicks = 80 + RNG.nextInt(60);
                                    // Start the blindfold timer
                                    TypePicker.MANAGER.setSpinning(target.getUuid(), spinTicks);

                                    ServerPlayNetworking.send(target, new SpinResult(assigned.get().id, spinTicks));

                                    src.sendFeedback(() -> Text.literal("Rerolling type for " + target.getName().getString()), true);
                                    return 1;
                                })
                        )
                )

                .then(literal("summon_npc")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            ServerWorld world = src.getWorld();
                            Vec3d pos = src.getPosition(); // Gets the exact coordinates of whoever ran the command

                            try {
                                EntityType<?> npcType = Registries.ENTITY_TYPE.get(Identifier.of("cobblemon", "npc"));
                                String nbtString = "{Tags:[\"msfortuneblack_npc\"], PersistenceRequired:1b, CustomName:'{\"text\":\"MSFortuneBlack\"}', CustomNameVisible:1b, npcConfiguration: {name: \"MSFortuneBlack\", modelType: \"player\", playerProfile: \"MSFortuneBlack\", movementState: \"WANDER\", lookState: \"WANDER\"}}";
                                NbtCompound nbt = StringNbtReader.parse(nbtString);

                                Entity npc = npcType.create(world);
                                if (npc != null) {
                                    npc.readNbt(nbt);
                                    // Drop the NPC exactly where the player is looking/standing
                                    npc.setPosition(pos.x, pos.y, pos.z);
                                    world.spawnEntity(npc);

                                    src.sendFeedback(() -> Text.literal("MSFortuneBlack has arrived."), true);
                                }
                            } catch (Exception e) {
                                src.sendError(Text.literal("Failed to summon NPC: " + e.getMessage()));
                            }
                            return 1;
                        })
                )

                // 4. /typepicker <player>
                .then(argument("player", StringArgumentType.word())
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            String name = StringArgumentType.getString(ctx, "player");
                            ServerPlayerEntity target = src.getServer().getPlayerManager().getPlayer(name);

                            if (target == null) {
                                src.sendError(Text.literal("Player not found: " + name));
                                return 0;
                            }

                            if (TypePicker.MANAGER.hasRole(target.getUuid())) {
                                src.sendError(Text.literal("That player already has a type. Use reroll."));
                                return 0;
                            }

                            Optional<Types> assigned = TypePicker.MANAGER.assignIfMissing(target.getUuid());
                            if (assigned.isEmpty()) {
                                src.sendError(Text.literal("No types left to assign."));
                                return 0;
                            }

                            TypePicker.MANAGER.save(src.getServer());

                            int spinTicks = 80 + RNG.nextInt(60);
                            // Start the blindfold timer
                            TypePicker.MANAGER.setSpinning(target.getUuid(), spinTicks);

                            ServerPlayNetworking.send(target, new SpinResult(assigned.get().id, spinTicks));

                            src.sendFeedback(() -> Text.literal("Rolling for " + target.getName().getString()), true);
                            return 1;
                        })
                )
        );
    }
}