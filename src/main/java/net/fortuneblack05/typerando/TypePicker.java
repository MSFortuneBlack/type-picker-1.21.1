package net.fortuneblack05.typerando;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fortuneblack05.typerando.Web.TypePickerWebServer;
import net.fortuneblack05.typerando.commands.TypePickerCommands;
import net.fortuneblack05.typerando.cobblemon.CobblemonIntegration;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class TypePicker implements ModInitializer {

	// Your manager instance
	public static final TypePickerManager MANAGER = new TypePickerManager();

	@Override
	public void onInitialize() {
		System.out.println("[TypeRando] Initializing...");

		// 2. Register Commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			TypePickerCommands.register(dispatcher);
		});

		// 3. Register the background timer that ticks down the spinning blindfolds
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MANAGER.tickSpins(server);
		});

		// 4. Safely check if Cobblemon exists before trying to hook into it
		if (FabricLoader.getInstance().isModLoaded("cobblemon")) {
			CobblemonIntegration.register();
			System.out.println("[TypeRando] Cobblemon detected! Integration active.");
		} else {
			System.out.println("[TypeRando] Cobblemon not found. Skipping integration.");
		}

		// 5. Hardcoded Vanilla NPC Spawner & JSON Loader
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {

			// Load your JSON save data when the server fully starts
			MANAGER.load(server);
			TypePickerWebServer.start(server);

			// --- Auto-Spawn MSFortuneBlack ---
			ServerWorld world = server.getOverworld();
			BlockPos spawnPos = world.getSpawnPos();

			boolean alreadyExists = false;

			// Check if we already spawned him (looks for a Villager named MSFortuneBlack)
			for (Entity entity : world.iterateEntities()) {
				if (entity.getType().equals(EntityType.VILLAGER) &&
						entity.getCustomName() != null &&
						entity.getCustomName().getString().equals("MSFortuneBlack")) {
					alreadyExists = true;
					break;
				}
			}

			// If he doesn't exist, build him at world spawn!
			if (!alreadyExists) {
				try {
					String nbtString = "{VillagerData:{profession:\"minecraft:nitwit\",level:2,type:\"minecraft:plains\"}, PersistenceRequired:1b, CustomName:'{\"text\":\"MSFortuneBlack\"}', CustomNameVisible:1b}";
					NbtCompound nbt = StringNbtReader.parse(nbtString);
					Entity npc = EntityType.VILLAGER.create(world);

					if (npc != null) {
						npc.readNbt(nbt);
						npc.setPosition(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
						world.spawnEntity(npc);
						System.out.println("[TypeRando] Spawned Vanilla wandering MSFortuneBlack NPC!");
					}
				} catch (Exception e) {
					System.err.println("[TypeRando] Failed to spawn NPC: " + e.getMessage());
				}
			}
		});

	}
}