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
import dev.isxander.adaptivetooltips.utils.SharedMixinData;
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
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Mixin(Screen.class)
public class ScreenMixin {
    @Shadow public int width;
    @Shadow public int height;

    @Shadow protected Font font;

    @Unique private boolean scissorActive;

    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/network/chat/Component;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V"))
    private void wrapText(Screen instance, PoseStack matrices, List<? extends FormattedCharSequence> lines, int x, int y, PoseStack dontuse, Component text) {
        SharedMixinData.alreadyWrapped = true;
        instance.renderTooltip(matrices, TooltipWrapper.wrapTooltipLines(instance, font, Collections.singletonList(text), x, DefaultTooltipPositioner.INSTANCE), x, y);
    }

    /**
     * Wraps an {@link FormattedCharSequence}.
     *
     * Wrapping an {@link FormattedCharSequence} is a lot more expensive than wrapping a {@link Component} object,
     * so we want to avoid doing it if possible. Hence the variable.
     */
    @Redirect(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private Stream<? extends FormattedCharSequence> wrapOrderedText(List<? extends FormattedCharSequence> instance, PoseStack matrices, List<? extends FormattedCharSequence> dontuse, int x, int y) {
        if (SharedMixinData.alreadyWrapped || AdaptiveTooltipConfig.INSTANCE.getConfig().wrapText == WrapTextBehaviour.OFF) // prevent back-and-forth conversion FormattedCharSequence -> Component -> FormattedCharSequence if wrapping isn't going to run anyway
            return instance.stream();
        SharedMixinData.alreadyWrapped = false;
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
        SharedMixinData.tooltipData = TooltipData.create(0, 0, width, height, this.width, this.height);
        Vector2ic currentPosition = operation.call(clientTooltipPositioner, screen, x, y, SharedMixinData.tooltipData.width(), SharedMixinData.tooltipData.height());
        SharedMixinData.tooltipData = SharedMixinData.tooltipData.withPosition(currentPosition.x(), currentPosition.y(), this.width, this.height);

        // push before returning so we don't need to repeat the check on pop
        matrices.pushPose(); // injection is before matrices.pushPose()

        if (clientTooltipPositioner instanceof DefaultTooltipPositioner || clientTooltipPositioner instanceof MenuTooltipPositioner || !AdaptiveTooltipConfig.INSTANCE.getConfig().onlyRepositionHoverTooltips) {
            for (TooltipPositionModule tooltipPositionModule : List.of(
                    new PrioritizeTooltipTopPositionModule(),
                    new BedrockCenteringPositionModule(),
                    new BestCornerPositionModule()
            )) {
                SharedMixinData.tooltipData = tooltipPositionModule.repositionTooltip(SharedMixinData.tooltipData, x, y, this.width, this.height);
            }
        }

        SharedMixinData.preScrollTransform = new Matrix4f(matrices.last().pose());

        SharedMixinData.edgeOffsetViewport = SharedMixinData.tooltipData.viewport().withOffset(4, this.width, this.height);

        ScrollTracker.scroll(matrices, components, SharedMixinData.tooltipData.tooltip(), SharedMixinData.edgeOffsetViewport);

        return new Vector2i(SharedMixinData.tooltipData.x(), SharedMixinData.tooltipData.y());
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", shift = At.Shift.AFTER))
    private void scissorText(PoseStack matrices, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return;

        this.scissorActive = true;

        float hPercent = ScrollTracker.getHorizontalScrollNormalized();
        float vPercent = ScrollTracker.getVerticalScrollNormalized();
        int hScrollbarHalfLength = Math.max(SharedMixinData.edgeOffsetViewport.width / 10, 10);
        int vScrollbarHalfLength = Math.max(SharedMixinData.edgeOffsetViewport.height / 10, 10);

        float x0 = SharedMixinData.edgeOffsetViewport.x + hPercent * (SharedMixinData.edgeOffsetViewport.width - hScrollbarHalfLength);
        float y0 = SharedMixinData.edgeOffsetViewport.y + vPercent * (SharedMixinData.edgeOffsetViewport.height - vScrollbarHalfLength);
        float x1 = x0 + hScrollbarHalfLength;
        float y1 = y0 + vScrollbarHalfLength;

        int scrollbarColor = AdaptiveTooltipConfig.INSTANCE.getConfig().scissorScrollbarColor.getRGB();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (SharedMixinData.tooltipData.tooltip().width > SharedMixinData.edgeOffsetViewport.width) {
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, x0, SharedMixinData.edgeOffsetViewport.y + SharedMixinData.edgeOffsetViewport.height, 400).color(scrollbarColor).endVertex();
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, x0, SharedMixinData.edgeOffsetViewport.y + SharedMixinData.edgeOffsetViewport.height + 3, 400).color(scrollbarColor).endVertex();
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, x1, SharedMixinData.edgeOffsetViewport.y + SharedMixinData.edgeOffsetViewport.height + 3, 400).color(scrollbarColor).endVertex();
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, x1, SharedMixinData.edgeOffsetViewport.y + SharedMixinData.edgeOffsetViewport.height, 400).color(scrollbarColor).endVertex();
        }

        if (SharedMixinData.tooltipData.tooltip().height > SharedMixinData.edgeOffsetViewport.height) {
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, SharedMixinData.edgeOffsetViewport.x + SharedMixinData.edgeOffsetViewport.width, y0, 400).color(scrollbarColor).endVertex();
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, SharedMixinData.edgeOffsetViewport.x + SharedMixinData.edgeOffsetViewport.width, y1, 400).color(scrollbarColor).endVertex();
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, SharedMixinData.edgeOffsetViewport.x + SharedMixinData.edgeOffsetViewport.width + 3, y1, 400).color(scrollbarColor).endVertex();
            bufferBuilder.vertex(SharedMixinData.preScrollTransform, SharedMixinData.edgeOffsetViewport.x + SharedMixinData.edgeOffsetViewport.width + 3, y0, 400).color(scrollbarColor).endVertex();
        }

        RenderSystem.enableDepthTest();
        BufferUploader.drawWithShader(bufferBuilder.end());

        GuiComponent.enableScissor(SharedMixinData.edgeOffsetViewport.x, SharedMixinData.edgeOffsetViewport.y, SharedMixinData.edgeOffsetViewport.x + SharedMixinData.edgeOffsetViewport.width, SharedMixinData.edgeOffsetViewport.y + SharedMixinData.edgeOffsetViewport.height);
    }

    @Inject(method = "renderTooltipInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 0))
    private void closeCustomMatrices(PoseStack matrices, List<ClientTooltipComponent> components, int x, int y, ClientTooltipPositioner positioner, CallbackInfo ci) {
        if (this.scissorActive) {
            GuiComponent.disableScissor();
        }

        matrices.popPose();
    }

    @ModifyConstant(method = "renderTooltipInternal", constant = @Constant(intValue = 2))
    private int removeFirstLinePadding(int padding) {
        if (AdaptiveTooltipConfig.INSTANCE.getConfig().removeFirstLinePadding)
            return 0;
        return padding;
    }
}
