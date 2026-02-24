package net.fortuneblack05.typerando.mixin;

import net.fortuneblack05.typerando.TypePicker;
import net.fortuneblack05.typerando.Types;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void formatDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (player.getWorld().isClient) {
            // --- CLIENT SIDE (3D Floating Nametags) -> Logo on the RIGHT ---
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());

                if (entry != null && entry.getDisplayName() != null) {
                    // Grab the raw text from the TAB menu
                    String rawTabName = entry.getDisplayName().getString();

                    if (!rawTabName.isEmpty()) {
                        char firstChar = rawTabName.charAt(0);

                        // Check if the first character is actually one of our logos
                        if (firstChar >= '\uE001' && firstChar <= '\uE012') {
                            String icon = String.valueOf(firstChar);

                            // Take the player's name and append the logo to the RIGHT
                            Text newName = cir.getReturnValue().copy()
                                    .append(Text.literal(" " + icon).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

                            cir.setReturnValue(newName);
                        }
                    }
                }
            }
        } else {
            // --- SERVER SIDE (Chat Messages) -> Logo on the LEFT ---
            Optional<Types> role = TypePicker.MANAGER.getRole(player.getUuid());
            if (role.isPresent()) {

                // Put the logo on the LEFT, then append the player's name
                Text newName = Text.literal(role.get().tabIcon + " ")
                        .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                        .append(cir.getReturnValue());

                cir.setReturnValue(newName);
            }
        }
    }
}