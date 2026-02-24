package net.fortuneblack05.typerando.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashTextRenderer;
import net.minecraft.client.resource.SplashTextResourceSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SplashTextResourceSupplier.class)
public class SplashTextMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void forceFortuneSplash(CallbackInfoReturnable<SplashTextRenderer> cir) {
        MinecraftClient client = MinecraftClient.getInstance();

        // Safety check to ensure the session exists
        if (client != null && client.getSession() != null) {
            String username = client.getSession().getUsername();

            // Check if it is the target player (ignores capitalization just in case)
            if ("MSFortuneBlack".equalsIgnoreCase(username)) {
                // Overwrite the random splash with our custom message
                cir.setReturnValue(new SplashTextRenderer("Go Stream ur ass Fortune"));
            }
        }
    }
}