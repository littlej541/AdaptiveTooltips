package dev.isxander.adaptivetooltips.mixins.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.helpers.TooltipWrapper;
import dev.isxander.adaptivetooltips.utils.GuiUtil;
import dev.isxander.adaptivetooltips.utils.SharedMixinData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(Screen.class)
public class ScreenMixin {
    @Shadow protected Font font;

    @Redirect(method = "renderComponentTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V"))
    private void wrapTextList(Screen instance, PoseStack matrices, List<? extends FormattedCharSequence> iHateOrderedText, int x, int y, PoseStack dontuse, List<Component> lines) {
        SharedMixinData.alreadyWrapped = true;
        instance.renderTooltip(matrices, TooltipWrapper.wrapTooltipLines(instance, font, lines, x, DefaultTooltipPositioner.INSTANCE), x, y);
    }

    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;Ljava/util/Optional;II)V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;", ordinal = 0))
    private Stream<FormattedCharSequence> wrapTextListWidthData(Stream<Component> instance, Function<Component, FormattedCharSequence> function, PoseStack matrices, List<Component> lines, Optional<TooltipComponent> data, int x, int y) {
        SharedMixinData.alreadyWrapped = true;
        return TooltipWrapper.wrapTooltipLines((Screen) (Object) this, font, instance.toList(), x, DefaultTooltipPositioner.INSTANCE).stream();
    }

    @ModifyArgs(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V"))
    private void clampTooltip(Args args) {
        if (AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips) {
            args.set(1, SharedMixinData.preScrollTransform);
            args.set(3, SharedMixinData.edgeOffsetViewport.x);
            args.set(4, SharedMixinData.edgeOffsetViewport.y);
            args.set(5, SharedMixinData.edgeOffsetViewport.width);
            args.set(6, SharedMixinData.edgeOffsetViewport.height);
        }
    }

    @ModifyArgs(method = "method_47943", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fillGradient(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V"))
    private static void changeTooltipColorAlpha(Args args) {
        args.set(7, GuiUtil.scaleAlpha(args.<Integer>get(7)));

        args.set(8, GuiUtil.scaleAlpha(args.<Integer>get(8)));
    }
}
