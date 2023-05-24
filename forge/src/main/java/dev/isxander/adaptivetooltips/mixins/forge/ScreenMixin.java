package dev.isxander.adaptivetooltips.mixins.forge;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.utils.GuiUtil;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Screen.class)
public class ScreenMixin {
    /**
     * Using {@link ModifyArgs} here causes a crash in Forge version, so it was split into multiple {@link ModifyArg}
     * mixins. There is probably a better way to do this, but this seems "easiest."
     */
    @ModifyArg(method = "lambda$renderTooltipInternal$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fillGradient(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V"), index = 7)
    private static int changeTooltipColorAlpha0(int color) {
        return GuiUtil.scaleAlpha(color);
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "lambda$renderTooltipInternal$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fillGradient(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V"), index = 8)
    private static int changeTooltipColorAlpha1(int color) {
        return GuiUtil.scaleAlpha(color);
    }
}
