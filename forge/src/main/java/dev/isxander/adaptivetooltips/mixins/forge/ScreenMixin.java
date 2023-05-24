package dev.isxander.adaptivetooltips.mixins.forge;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.utils.GuiUtil;
import dev.isxander.adaptivetooltips.utils.SharedMixinData;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Screen.class)
public class ScreenMixin {
    /**
     * Using {@link org.spongepowered.asm.mixin.injection.ModifyArgs} here causes a crash in Forge version, so it was split into multiple {@link ModifyArg}
     * mixins. There is probably a better way to do this, but this seems "easiest."
     */
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIIIII)V"), index = 1)
    private Matrix4f clampTooltipBackground_Transform(Matrix4f matrix4f) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return matrix4f;

        return SharedMixinData.preScrollTransform;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIIIII)V"), index = 3)
    private int clampTooltipBackground_X(int x) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return x;

        return SharedMixinData.edgeOffsetViewport.x;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIIIII)V"), index = 4)
    private int clampTooltipBackground_Y(int y) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return y;

        return SharedMixinData.edgeOffsetViewport.y;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIIIII)V"), index = 5)
    private int clampTooltipBackground_Width(int width) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return width;

        return SharedMixinData.edgeOffsetViewport.width;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIIIII)V"), index = 6)
    private int clampTooltipBackground_Height(int height) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return height;

        return SharedMixinData.edgeOffsetViewport.height;
    }

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
