package dev.isxander.adaptivetooltips.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.config.WrapTextBehaviour;
import dev.isxander.adaptivetooltips.helpers.ScrollTracker;
import dev.isxander.adaptivetooltips.helpers.TooltipData;
import dev.isxander.adaptivetooltips.helpers.TooltipWrapper;
import dev.isxander.adaptivetooltips.helpers.positioner.BedrockCenteringPositionModule;
import dev.isxander.adaptivetooltips.helpers.positioner.BestCornerPositionModule;
import dev.isxander.adaptivetooltips.helpers.positioner.PrioritizeTooltipTopPositionModule;
import dev.isxander.adaptivetooltips.helpers.positioner.TooltipPositionModule;
import dev.isxander.adaptivetooltips.utils.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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

    @Unique private TooltipData tooltipData;
    @Unique private TooltipData.Viewport edgeOffsetViewport;

    @Unique private boolean scissorActive;
    @Unique private Matrix4f preScrollTransform;

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
        this.tooltipData = TooltipData.create(0, 0, width, height, this.width, this.height);
        Vector2ic currentPosition = operation.call(clientTooltipPositioner, screen, x, y, this.tooltipData.width(), this.tooltipData.height());
        this.tooltipData = this.tooltipData.withPosition(currentPosition.x(), currentPosition.y(), this.width, this.height);

        // push before returning so we don't need to repeat the check on pop
        matrices.pushPose(); // injection is before matrices.pushPose()

        if (clientTooltipPositioner instanceof DefaultTooltipPositioner || clientTooltipPositioner instanceof MenuTooltipPositioner || !AdaptiveTooltipConfig.INSTANCE.getConfig().onlyRepositionHoverTooltips) {
            for (TooltipPositionModule tooltipPositionModule : List.of(
                    new PrioritizeTooltipTopPositionModule(),
                    new BedrockCenteringPositionModule(),
                    new BestCornerPositionModule()
            )) {
                this.tooltipData = tooltipPositionModule.repositionTooltip(this.tooltipData, x, y, this.width, this.height);
            }
        }
        
        this.preScrollTransform = new Matrix4f(matrices.last().pose());

        this.edgeOffsetViewport = this.tooltipData.viewport().withOffset(4, this.width, this.height);

        ScrollTracker.scroll(matrices, components, this.tooltipData.tooltip(), this.edgeOffsetViewport);

        return new Vector2i(tooltipData.x(), tooltipData.y());
    }

    /**
     * Using {@link ModifyArgs} here causes a crash in Forge version, so it was split into multiple {@link ModifyArg}
     * mixins. There is probably a better way to do this, but this seems "easiest." There is a patched version of
     * {@link ModifyArgs} and {@link Args} in the Forge project provided by Architectury that may be the intended
     * solution, but that would require some refactoring the scope of which is unknown.
     */
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V"), index = 1)
    private Matrix4f clampTooltipBackground_Transform(Matrix4f matrix4f) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return matrix4f;

        return this.preScrollTransform;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V"), index = 3)
    private int clampTooltipBackground_X(int x) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return x;

        return this.edgeOffsetViewport.x;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V"), index = 4)
    private int clampTooltipBackground_Y(int y) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return y;

        return this.edgeOffsetViewport.y;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V"), index = 5)
    private int clampTooltipBackground_Width(int width) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return width;

        return this.edgeOffsetViewport.width;
    }

    // Forge ModifyArgs workaround, see above
    @ModifyArg(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;renderTooltipBackground(Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil$BlitPainter;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/BufferBuilder;IIIII)V"), index = 6)
    private int clampTooltipBackground_Height(int height) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return height;

        return this.edgeOffsetViewport.height;
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", shift = At.Shift.AFTER))
    private void scissorText(PoseStack matrices, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return;

        this.scissorActive = true;

        float vPercent = ScrollTracker.getVerticalScrollNormalized();
        float hPercent = ScrollTracker.getHorizontalScrollNormalized();
        float x0 = edgeOffsetViewport.x + hPercent * (edgeOffsetViewport.width - 16);
        float y0 = edgeOffsetViewport.y + vPercent * (edgeOffsetViewport.height - 16);
        float x1 = x0 + 16;
        float y1 = y0 + 16;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (tooltipData.tooltip().width > edgeOffsetViewport.width) {
            bufferBuilder.vertex(this.preScrollTransform, x0, edgeOffsetViewport.y + edgeOffsetViewport.height + 1, 400).color(1f, 0f, 0f, 1f).endVertex();
            bufferBuilder.vertex(this.preScrollTransform, x0, edgeOffsetViewport.y + edgeOffsetViewport.height + 3, 400).color(1f, 0f, 0f, 1f).endVertex();
            bufferBuilder.vertex(this.preScrollTransform, x1, edgeOffsetViewport.y + edgeOffsetViewport.height + 3, 400).color(1f, 0f, 0f, 1f).endVertex();
            bufferBuilder.vertex(this.preScrollTransform, x1, edgeOffsetViewport.y + edgeOffsetViewport.height + 1, 400).color(1f, 0f, 0f, 1f).endVertex();
        }

        if (tooltipData.tooltip().height > edgeOffsetViewport.height) {
            bufferBuilder.vertex(this.preScrollTransform, edgeOffsetViewport.x + edgeOffsetViewport.width + 1, y0, 400).color(1f, 0f, 0f, 1f).endVertex();
            bufferBuilder.vertex(this.preScrollTransform, edgeOffsetViewport.x + edgeOffsetViewport.width + 1, y1, 400).color(1f, 0f, 0f, 1f).endVertex();
            bufferBuilder.vertex(this.preScrollTransform, edgeOffsetViewport.x + edgeOffsetViewport.width + 3, y1, 400).color(1f, 0f, 0f, 1f).endVertex();
            bufferBuilder.vertex(this.preScrollTransform, edgeOffsetViewport.x + edgeOffsetViewport.width + 3, y0, 400).color(1f, 0f, 0f, 1f).endVertex();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferUploader.drawWithShader(bufferBuilder.end());

        GuiComponent.enableScissor(edgeOffsetViewport.x, edgeOffsetViewport.y, edgeOffsetViewport.x + edgeOffsetViewport.width, edgeOffsetViewport.y + edgeOffsetViewport.height);
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void closeCustomMatrices(PoseStack matrices, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        if (this.scissorActive) {
            GuiComponent.disableScissor();
        }

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
