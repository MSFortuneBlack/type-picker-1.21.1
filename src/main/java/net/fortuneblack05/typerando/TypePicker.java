package net.fortuneblack05.typerando;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fortuneblack05.typerando.Web.TypePickerWebServer;
import net.fortuneblack05.typerando.commands.TypePickerCommands;

public class TypePicker implements ModInitializer {
	public static final TypePickerManager MANAGER = new TypePickerManager();

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				TypePickerCommands.register(dispatcher)
		);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			MANAGER.load(server);
			TypePickerWebServer.start(server); // <-- Starts the Web Server
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			MANAGER.tickSpins(server);
		});	

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			MANAGER.save(server);
			TypePickerWebServer.stop(); // <-- Safely stops it
		});

		System.out.println("[TypeRando] Loaded.");
	}
}