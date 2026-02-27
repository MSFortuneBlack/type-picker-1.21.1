package net.fortuneblack05.typerando.Client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fortuneblack05.typerando.cobblemon.CobblemonIntegration;
import net.fortuneblack05.typerando.payloads.RollSummary;
import net.fortuneblack05.typerando.payloads.SpinResult;
import net.fortuneblack05.typerando.Types;

public class TypePickerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(SpinResult.ID, SpinResult.CODEC);
        PayloadTypeRegistry.playS2C().register(RollSummary.ID, RollSummary.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(SpinResult.ID, (payload, context) -> {
            context.client().execute(() -> {
                Types type = Types.fromId(payload.typeId()).orElse(Types.NORMAL);
                TypePickerOverlay.startSpin(type, payload.spinTicks());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RollSummary.ID, (payload, context) -> {
            context.client().execute(() ->
                    TypePickerOverlay.startSummary(payload.names(), payload.typeIds())
            );
        });

        // Register our overlay events
        TypePickerOverlay.register();
    }
}