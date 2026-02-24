package net.fortuneblack05.typerando.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void syncNametagWithTab(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // We only intercept this on the Client side (where 3D graphics are drawn)
        if (player.getWorld().isClient) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                // Look up this specific player in the TAB list data
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());

                // If they have a custom TAB name (which contains our logo), use it!
                if (entry != null && entry.getDisplayName() != null) {
                    cir.setReturnValue(entry.getDisplayName());
                }
            }
        }
    }
}