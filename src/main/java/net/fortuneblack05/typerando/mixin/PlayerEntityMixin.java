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
            // --- CLIENT SIDE (3D Floating Nametags) ---
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());

                if (entry != null && entry.getDisplayName() != null) {
                    String rawTabName = entry.getDisplayName().getString();

                    if (!rawTabName.isEmpty()) {
                        char firstChar = rawTabName.charAt(0);

                        // Check if the TAB list has one of the base 1-18 logos
                        if (firstChar >= '\uE001' && firstChar <= '\uE012') {

                            // SHIFT TRICK: Add 18 to the character code to get the "a" variant!
                            char shiftedChar = (char) (firstChar + 18);
                            String icon3D = String.valueOf(shiftedChar);

                            // Append the new shifted 3D logo to the RIGHT side of the name
                            Text newName = cir.getReturnValue().copy()
                                    .append(Text.literal(" " + icon3D).setStyle(Style.EMPTY.withColor(Formatting.WHITE)));

                            cir.setReturnValue(newName);
                        }
                    }
                }
            }
        } else {
            // --- SERVER SIDE (Chat Messages) -> Logo on the LEFT ---
            Optional<Types> role = TypePicker.MANAGER.getRole(player.getUuid());
            if (role.isPresent()) {
                Text newName = Text.literal(role.get().tabIcon + " ")
                        .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                        .append(cir.getReturnValue());

                cir.setReturnValue(newName);
            }
        }
    }
}