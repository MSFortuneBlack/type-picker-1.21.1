package net.fortuneblack05.typerando.mixin;

import net.fortuneblack05.typerando.TypePicker;
import net.fortuneblack05.typerando.Types;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Inject(method = "getPlayerListName", at = @At("HEAD"), cancellable = true)
    private void customTabName(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Optional<Types> role = TypePicker.MANAGER.getRole(player.getUuid());

        if (role.isPresent()) {
            Text tabName = Text.literal(role.get().tabIcon + " ")
                    .setStyle(Style.EMPTY.withColor(Formatting.WHITE))
                    .append(player.getName());
            cir.setReturnValue(tabName);
        }
    }
}