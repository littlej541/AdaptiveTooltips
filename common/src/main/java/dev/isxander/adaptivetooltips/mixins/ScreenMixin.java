package dev.isxander.adaptivetooltips.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.config.WrapTextBehaviour;
import dev.isxander.adaptivetooltips.helpers.ScrollTracker;
import dev.isxander.adaptivetooltips.helpers.TooltipWrapper;
import dev.isxander.adaptivetooltips.helpers.positioner.BedrockCenteringPositionModule;
import dev.isxander.adaptivetooltips.helpers.positioner.BestCornerPositionModule;
import dev.isxander.adaptivetooltips.helpers.positioner.PrioritizeTooltipTopPositionModule;
import dev.isxander.adaptivetooltips.helpers.positioner.TooltipPositionModule;
import dev.isxander.adaptivetooltips.utils.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.awt.Color;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Mixin(Screen.class)
public class ScreenMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Shadow protected Font font;

    @Unique private boolean debugify$alreadyWrapped = false;

    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V"))
    private void wrapText(Screen instance, PoseStack matrices, List<? extends FormattedCharSequence> lines, int x, int y, PoseStack dontuse, Component text) {
        debugify$alreadyWrapped = true;
        instance.renderTooltip(matrices, TooltipWrapper.wrapTooltipLines(instance, font, Collections.singletonList(text), x, DefaultTooltipPositioner.INSTANCE), x, y);
    }

    @Redirect(method = "renderComponentTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V"))
    private void wrapTextList(Screen instance, PoseStack matrices, List<? extends FormattedCharSequence> iHateOrderedText, int x, int y, PoseStack dontuse, List<Component> lines) {
        debugify$alreadyWrapped = true;
        instance.renderTooltip(matrices, TooltipWrapper.wrapTooltipLines(instance, font, lines, x, DefaultTooltipPositioner.INSTANCE), x, y);
    }

    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;Ljava/util/Optional;II)V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;", ordinal = 0))
    private Stream<FormattedCharSequence> wrapTextListWidthData(Stream<Component> instance, Function<Component, FormattedCharSequence> function, PoseStack matrices, List<Component> lines, Optional<TooltipComponent> data, int x, int y) {
        debugify$alreadyWrapped = true;
        return TooltipWrapper.wrapTooltipLines((Screen) (Object) this, font, instance.toList(), x, DefaultTooltipPositioner.INSTANCE).stream();
    }

    /**
     * Wraps an {@link FormattedCharSequence}.
     *
     * Wrapping an {@link FormattedCharSequence} is a lot more expensive than wrapping a {@link Component} object,
     * so we want to avoid doing it if possible. Hence the variable.
     */
    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private Stream<? extends FormattedCharSequence> wrapOrderedText(List<? extends FormattedCharSequence> instance, PoseStack matrices, List<? extends FormattedCharSequence> dontuse, int x, int y) {
        if (debugify$alreadyWrapped || AdaptiveTooltipConfig.INSTANCE.getConfig().wrapText == WrapTextBehaviour.OFF) // prevent back-and-forth conversion FormattedCharSequence -> Component -> FormattedCharSequence if wrapping isn't going to run anyway
            return instance.stream();
        debugify$alreadyWrapped = false;
        return TooltipWrapper.wrapTooltipLines((Screen) (Object) this, font, TextUtil.toText(instance), x, DefaultTooltipPositioner.INSTANCE).stream();
    }

    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/screens/Screen$DeferredTooltipRendering;II)V", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private Stream<? extends FormattedCharSequence> wrapPositionedOrderedText(List<? extends FormattedCharSequence> instance, PoseStack matrices, Screen.DeferredTooltipRendering positionedTooltip, int x, int y) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().overwriteVanillaWrapping || AdaptiveTooltipConfig.INSTANCE.getConfig().wrapText == WrapTextBehaviour.OFF) // prevent back-and-forth conversion FormattedCharSequence -> Component -> FormattedCharSequence if wrapping isn't going to run anyway
            return instance.stream();
        return TooltipWrapper.wrapTooltipLines((Screen) (Object) this, font, TextUtil.toText(instance), x, positionedTooltip.positioner()).stream();
    }

    @WrapOperation(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(Lnet/minecraft/client/gui/screens/Screen;IIII)Lorg/joml/Vector2ic;"))
    private Vector2ic moveTooltip(ClientTooltipPositioner clientTooltipPositioner, Screen screen, int x, int y, int width, int height, Operation<Vector2ic> operation, PoseStack matrices, List<ClientTooltipComponent> components, int mouseX, int mouseY) {
        Vector2ic currentPosition = operation.call(clientTooltipPositioner, screen, x, y, width, height);

        // push before returning so we don't need to repeat the check on pop
        matrices.pushPose(); // injection is before matrices.pushPose()

        if (!(clientTooltipPositioner instanceof DefaultTooltipPositioner || clientTooltipPositioner instanceof MenuTooltipPositioner) && AdaptiveTooltipConfig.INSTANCE.getConfig().onlyRepositionHoverTooltips) {
            return currentPosition;
        }

        for (TooltipPositionModule tooltipPositionModule : List.of(
                new PrioritizeTooltipTopPositionModule(),
                new BedrockCenteringPositionModule(),
                new BestCornerPositionModule()
        )) {
            Optional<Vector2ic> position = tooltipPositionModule.repositionTooltip(currentPosition.x(), currentPosition.y(), width, height, mouseX, mouseY, this.width, this.height);
            if (position.isPresent())
                currentPosition = position.get();
        }

        ScrollTracker.scroll(matrices, components, currentPosition.x(), currentPosition.y(), width, height, this.width, this.height);

        return currentPosition;
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void closeCustomMatrices(PoseStack matrices, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        matrices.popPose();
    }

    @ModifyArgs(method = "method_47943", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiComponent;fillGradient(Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIIIII)V"))
    private static void changeTooltipColorAlpha(Args args) {
        Color colorStart = new Color(args.<Integer>get(7), true);
        args.set(7, new Color(
                colorStart.getRed(),
                colorStart.getGreen(),
                colorStart.getBlue(),
                (int) Mth.clamp(
                        colorStart.getAlpha() * AdaptiveTooltipConfig.INSTANCE.getConfig().tooltipTransparency,
                        0, 255)
        ).getRGB());

        Color colorEnd = new Color(args.<Integer>get(8), true);
        args.set(8, new Color(
                colorEnd.getRed(),
                colorEnd.getGreen(),
                colorEnd.getBlue(),
                (int) Mth.clamp(
                        colorEnd.getAlpha() * AdaptiveTooltipConfig.INSTANCE.getConfig().tooltipTransparency,
                        0, 255)
        ).getRGB());
    }

    @ModifyConstant(method = "renderTooltipInternal", constant = @Constant(intValue = 2))
    private int removeFirstLinePadding(int padding) {
        if (AdaptiveTooltipConfig.INSTANCE.getConfig().removeFirstLinePadding)
            return 0;
        return padding;
    }
}
