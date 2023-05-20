package dev.isxander.adaptivetooltips.helpers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.config.ScrollDirection;
import dev.isxander.adaptivetooltips.config.WrapTextBehaviour;
import dev.isxander.adaptivetooltips.mixins.ClientBundleTooltipAccessor;
import dev.isxander.adaptivetooltips.mixins.ClientTextTooltipAccessor;
import dev.isxander.adaptivetooltips.utils.TextUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ScrollTracker {
    private static int verticalScrollAccumulator = 0;
    private static int horizontalScrollAccumulator = 0;

    private static float targetVerticalScroll = 0f;
    private static float targetHorizontalScroll = 0f;

    private static float currentVerticalScroll = 0f;
    private static float currentHorizontalScroll = 0f;

    private static float targetVerticalScrollNormalized = 0f;
    private static float targetHorizontalScrollNormalized = 0f;

    private static float currentVerticalScrollNormalized = 0f;
    private static float currentHorizontalScrollNormalized = 0f;

    private static Range verticalRange = null;
    private static Range horizontalRange = null;
    private static int offsetX = 0;
    private static int offsetY = 0;
    private static List<ClientTooltipComponent> trackedComponents = null;
    private static String trackedComponentsString = "";


    public static boolean renderedThisFrame = false;

    public static void addVerticalScroll(int amt) {
        if (AdaptiveTooltipConfig.INSTANCE.getConfig().scrollDirection == ScrollDirection.REVERSE)
            amt = -amt;
        verticalScrollAccumulator += amt * AdaptiveTooltipConfig.INSTANCE.getConfig().verticalScrollSensitivity;
    }

    public static void addHorizontalScroll(int amt) {
        if (AdaptiveTooltipConfig.INSTANCE.getConfig().scrollDirection == ScrollDirection.REVERSE)
            amt = -amt;
        horizontalScrollAccumulator += amt * AdaptiveTooltipConfig.INSTANCE.getConfig().horizontalScrollSensitivity;
    }

    public static float getVerticalScroll() {
        return currentVerticalScroll;
    }

    public static float getHorizontalScroll() {
        return currentHorizontalScroll;
    }

    public static float getVerticalScrollNormalized() {
        return currentVerticalScrollNormalized;
    }

    public static float getHorizontalScrollNormalized() {
        return currentHorizontalScrollNormalized;
    }

    public static void scroll(PoseStack matrices, List<ClientTooltipComponent> components, TooltipData.Tooltip tooltip, TooltipData.Viewport viewport) {
        calculateScrollData(tooltip, viewport);

        tick(components, Minecraft.getInstance().getDeltaFrameTime());

        // have to use a translate rather than moving the tooltip's x and y because int precision is too jittery
        matrices.translate(-currentHorizontalScroll, -currentVerticalScroll, 0f);
    }

    private static void tick(List<ClientTooltipComponent> components, float tickDelta) {
        renderedThisFrame = true;

        resetIfNeeded(components);

        // attempt to keep scroll in same position for remaining width tooltips
        targetVerticalScroll = lerpWithRange(targetVerticalScrollNormalized, verticalRange);
        targetHorizontalScroll = lerpWithRange(targetHorizontalScrollNormalized, horizontalRange);
        currentVerticalScroll = lerpWithRange(currentVerticalScrollNormalized, verticalRange);
        currentHorizontalScroll = lerpWithRange(currentHorizontalScrollNormalized, horizontalRange);

        // prevents scrolling too far up/down
        targetVerticalScroll = clampWithRange(targetVerticalScroll + verticalScrollAccumulator, verticalRange);
        targetHorizontalScroll = clampWithRange(targetHorizontalScroll + horizontalScrollAccumulator, horizontalRange);

        verticalScrollAccumulator = horizontalScrollAccumulator = 0;

        // save normalized scroll position for attempts at keeping scroll in the same place when moved
        targetVerticalScrollNormalized = inverseLerpWithRange(targetVerticalScroll, verticalRange);
        targetHorizontalScrollNormalized = inverseLerpWithRange(targetHorizontalScroll, horizontalRange);

        tickAnimation(tickDelta);

        // same as above but we do this after animation since values are altered by lerp there
        currentVerticalScrollNormalized = inverseLerpWithRange(currentVerticalScroll, verticalRange);
        currentHorizontalScrollNormalized = inverseLerpWithRange(currentHorizontalScroll, horizontalRange);
    }

    private static void tickAnimation(float tickDelta) {
        if (AdaptiveTooltipConfig.INSTANCE.getConfig().smoothScrolling) {
            currentVerticalScroll = Mth.lerp(tickDelta * 0.5f, currentVerticalScroll, targetVerticalScroll);
            currentHorizontalScroll = Mth.lerp(tickDelta * 0.5f, currentHorizontalScroll, targetHorizontalScroll);
        } else {
            currentVerticalScroll = targetVerticalScroll;
            currentHorizontalScroll = targetHorizontalScroll;
        }
    }

    private static void resetIfNeeded(List<ClientTooltipComponent> components) {
        // if not the same component as last frame, reset the scrolling.
        if (isEqual(components)) {
            return;
        }

        reset();

        trackedComponents = components;

        // save tooltip string with no whitespace characters for matching against
        // basically just for remaining with configuration
        if (components.stream().anyMatch(component -> !(component instanceof ClientTextTooltip))) {
            trackedComponentsString = "";
        } else {
            trackedComponentsString = ((List<ClientTextTooltip>)(Object)components)
                    .stream()
                    .map(component -> ((ClientTextTooltipAccessor) component).getText())
                    .map(TextUtil::toText)
                    .map(component -> StringUtils.deleteWhitespace(component.getString()))
                    .collect(Collectors.joining());
        }
    }

    public static void reset() {
        targetVerticalScroll = targetHorizontalScroll = 0f;
        targetVerticalScrollNormalized = targetHorizontalScrollNormalized = 0f;
        verticalScrollAccumulator = horizontalScrollAccumulator = 0;
        currentVerticalScroll = currentHorizontalScroll = 0f;
        currentVerticalScrollNormalized = currentHorizontalScrollNormalized = 0f;

        // set proper scroll position on reset so we the tooltip doesn't "woosh" when it opens
        if (verticalRange != null && horizontalRange != null) {
            targetVerticalScrollNormalized = currentVerticalScrollNormalized = inverseLerpWithRange(offsetY, verticalRange.normalized());
            targetHorizontalScrollNormalized = currentHorizontalScrollNormalized = inverseLerpWithRange(offsetX, horizontalRange.normalized());
            targetVerticalScroll = currentVerticalScroll = clampWithRange(lerpWithRange(currentVerticalScrollNormalized, verticalRange), verticalRange);
            targetHorizontalScroll = currentHorizontalScroll = clampWithRange(lerpWithRange(currentHorizontalScrollNormalized, horizontalRange), horizontalRange);
        }
    }

    // test for equality regardless of wrapping
    private static boolean wrappedEqual(List<? extends ClientTooltipComponent> l1) {
        if (l1 == null || trackedComponents == null || l1.stream().anyMatch(component -> !(component instanceof ClientTextTooltip))) {
            return false;
        }

        String wholeString = ((List<ClientTextTooltip>)l1)
                .stream()
                .map(component -> ((ClientTextTooltipAccessor) component).getText())
                .map(TextUtil::toText)
                .map(component -> StringUtils.deleteWhitespace(component.getString()))
                .collect(Collectors.joining());

        return wholeString.equals(trackedComponentsString);
    }

    private static boolean isEqual(List<ClientTooltipComponent> l1) {
        List<ClientTooltipComponent> l2 = trackedComponents;

        if (l1 == null || l2 == null)
            return false;

        if (AdaptiveTooltipConfig.INSTANCE.getConfig().wrapText == WrapTextBehaviour.REMAINING_WIDTH && wrappedEqual(l1)) {
            return true;
        }

        Iterator<ClientTooltipComponent> iter1 = l1.iterator();
        Iterator<ClientTooltipComponent> iter2 = l2.iterator();

        // loop through both lists until either ends
        while (iter1.hasNext() && iter2.hasNext()) {
            ClientTooltipComponent c1 = iter1.next();
            ClientTooltipComponent c2 = iter2.next();

            // if the components are same instance, they are the same, go to next element
            if (c1 == c2) continue;

            // no abstract way of comparing tooltip components so we have to check what implementation they are
            if (c1 instanceof ClientTextTooltip ot1 && c2 instanceof ClientTextTooltip ot2) {
                // OrderedText cannot be compared, MutableText can
                if (!TextUtil.toText(((ClientTextTooltipAccessor) ot1).getText()).equals(TextUtil.toText(((ClientTextTooltipAccessor) ot2).getText())))
                    return false;
            } else if (c1 instanceof ClientBundleTooltip bt1 && c2 instanceof ClientBundleTooltip bt2) {
                // gets the inventory of each bundle and loops through each stack

                Iterator<ItemStack> i1 = ((ClientBundleTooltipAccessor) bt1).getItems().iterator();
                Iterator<ItemStack> i2 = ((ClientBundleTooltipAccessor) bt2).getItems().iterator();

                // iterate through both bundle inventories until either runs out
                while (i1.hasNext() && i2.hasNext()) {
                    ItemStack stack1 = i1.next();
                    ItemStack stack2 = i2.next();

                    if (!ItemStack.matches(stack1, stack2))
                        return false;
                }

                // if either inventory has more items, we know they are not the same inventory
                if (i1.hasNext() || i2.hasNext())
                    return false;
            } else {
                // no other vanilla implementations of TooltipComponent or the two components are different to eachother
                return false;
            }
        }

        return !(iter1.hasNext() || iter2.hasNext());
    }

    private static float clampWithRange(float current, Range range) {
        return Mth.clamp(current, range.min, range.max);
    }

    private static float lerpWithRange(float delta, Range range) {
        return Mth.lerp(delta, range.min, range.max);
    }

    private static float inverseLerpWithRange(float current, Range range) {
        return range.min - range.max != 0f ? Mth.inverseLerp(current, range.min, range.max) : 0f;
    }

    private static void calculateScrollData(TooltipData.Tooltip tooltip, TooltipData.Viewport viewport) {
        verticalRange = new Range(
                Math.min(-viewport.y + tooltip.y, 0),
                Math.max(tooltip.height - viewport.y + tooltip.y - viewport.height, 0)
        );
        horizontalRange = new Range(
                Math.min(-viewport.x + tooltip.x, 0),
                Math.max(tooltip.width - viewport.x + tooltip.x - viewport.width, 0)
        );

        offsetY = viewport.y - tooltip.y;
        offsetX = viewport.x - tooltip.x;
    }

    private record Range(float min, float max) {
        public float range() {
            return this.max - this.min;
        }

        public Range normalized() {
            return new Range(0f, this.range());
        }
    }
}
