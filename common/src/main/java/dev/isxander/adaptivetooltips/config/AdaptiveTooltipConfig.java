package dev.isxander.adaptivetooltips.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.adaptivetooltips.config.gui.KeyCodeController;
import dev.isxander.adaptivetooltips.platform.services.Services;
import dev.isxander.yacl.api.Binding;
import dev.isxander.yacl.api.ConfigCategory;
import dev.isxander.yacl.api.Option;
import dev.isxander.yacl.api.OptionGroup;
import dev.isxander.yacl.api.YetAnotherConfigLib;
import dev.isxander.yacl.config.ConfigEntry;
import dev.isxander.yacl.config.GsonConfigInstance;
import dev.isxander.yacl.gui.controllers.LabelController;
import dev.isxander.yacl.gui.controllers.TickBoxController;
import dev.isxander.yacl.gui.controllers.cycling.EnumController;
import dev.isxander.yacl.gui.controllers.slider.FloatSliderController;
import dev.isxander.yacl.gui.controllers.slider.IntegerSliderController;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.awt.Color;

public class AdaptiveTooltipConfig {
    public static final GsonConfigInstance<AdaptiveTooltipConfig> INSTANCE = GsonConfigInstance
            .createBuilder(AdaptiveTooltipConfig.class)
            .setPath(Services.PLATFORM.getConfigPath().resolve("adaptive-tooltips.json"))
            .overrideGsonBuilder(new GsonBuilder()
                    .registerTypeHierarchyAdapter(Component.class, new Component.Serializer())
                    .registerTypeHierarchyAdapter(Style.class, new Style.Serializer())
                    .registerTypeHierarchyAdapter(Color.class, new GsonConfigInstance.ColorTypeAdapter())
                    .serializeNulls()
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .setPrettyPrinting())
            .build();

    @ConfigEntry public WrapTextBehaviour wrapText = WrapTextBehaviour.SCREEN_WIDTH;
    @ConfigEntry public boolean overwriteVanillaWrapping = false;
    @ConfigEntry public boolean prioritizeTooltipTop = true;
    @ConfigEntry public boolean bedrockCentering = true;
    @ConfigEntry public boolean bestCorner = false;
    @ConfigEntry public boolean alwaysBestCorner = false;
    @ConfigEntry public boolean preventVanillaClamping = true;
    @ConfigEntry public boolean onlyRepositionHoverTooltips = true;
    @ConfigEntry public boolean useYACLTooltipPositioner = false;
    @ConfigEntry public int scrollKeyCode = InputConstants.KEY_LALT;
    @ConfigEntry public int horizontalScrollKeyCode = InputConstants.KEY_LCONTROL;
    @ConfigEntry public boolean smoothScrolling = true;
    @ConfigEntry public ScrollDirection scrollDirection = Util.getPlatform() == Util.OS.OSX ? ScrollDirection.NATURAL : ScrollDirection.REVERSE;
    @ConfigEntry public int verticalScrollSensitivity = 10;
    @ConfigEntry public int horizontalScrollSensitivity = 10;
    @ConfigEntry public boolean scissorTooltips = false;
    @ConfigEntry public boolean scissorIsSize = true;
    @ConfigEntry public int clampHeight = 100;
    @ConfigEntry public float tooltipTransparency = 1f;
    @ConfigEntry public boolean removeFirstLinePadding = true;

    public static Screen makeScreen(Screen parent) {
        return YetAnotherConfigLib.create(INSTANCE, (defaults, config, builder) -> {
            var categoryBuilder = ConfigCategory.createBuilder()
                    .name(Component.translatable("adaptivetooltips.title"));

            var contentManipulationGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("adaptivetooltips.group.content_manipulation.title"))
                    .tooltip(Component.translatable("adaptivetooltips.group.content_manipulation.desc"));
            var textWrappingOpt = Option.createBuilder(WrapTextBehaviour.class)
                    .name(Component.translatable("adaptivetooltips.opt.text_wrapping.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.text_wrapping.desc"))
                    .tooltip(value -> {
                        MutableComponent tooltip = Component.translatable("options.generic_value", value.getDisplayName(), value.getTooltip());
                        if (value == WrapTextBehaviour.REMAINING_WIDTH)
                            tooltip.append("\n").append(Component.translatable("adaptivetooltips.wrap_text_behaviour.remaining_width.warning").withStyle(ChatFormatting.RED));
                        return tooltip.withStyle(ChatFormatting.GRAY);
                    })
                    .binding(
                            defaults.wrapText,
                            () -> config.wrapText,
                            val -> config.wrapText = val
                    )
                    .controller(EnumController::new)
                    .build();
            var preventVanillaWrappingOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.overwrite_vanilla_wrapping.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.overwrite_vanilla_wrapping.desc"))
                    .binding(
                            defaults.overwriteVanillaWrapping,
                            () -> config.overwriteVanillaWrapping,
                            val -> config.overwriteVanillaWrapping = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            contentManipulationGroup.option(textWrappingOpt);
            contentManipulationGroup.option(preventVanillaWrappingOpt);
            categoryBuilder.group(contentManipulationGroup.build());

            var positioningGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("adaptivetooltips.group.positioning.title"))
                    .tooltip(Component.translatable("adaptivetooltips.group.positioning.desc"));
            var prioritizeTooltipTopOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.prioritize_tooltip_top.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.prioritize_tooltip_top.desc"))
                    .binding(
                            defaults.prioritizeTooltipTop,
                            () -> config.prioritizeTooltipTop,
                            val -> config.prioritizeTooltipTop = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            var bedrockCenteringOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.bedrock_centering.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.bedrock_centering.desc"))
                    .tooltip(Component.translatable("adaptivetooltips.gui.require_opt.on", Component.translatable("adaptivetooltips.opt.prevent_vanilla_clamping.title")).withStyle(ChatFormatting.RED))
                    .binding(
                            defaults.bedrockCentering,
                            () -> config.bedrockCentering,
                            val -> config.bedrockCentering = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            var alwaysAlignToCornerOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.always_align_corner.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.always_align_corner.desc"))
                    .tooltip(Component.translatable("adaptivetooltips.gui.require_opt.on", Component.translatable("adaptivetooltips.opt.align_to_corner.title")).withStyle(ChatFormatting.RED))
                    .binding(
                            defaults.alwaysBestCorner,
                            () -> config.alwaysBestCorner,
                            val -> config.alwaysBestCorner = val
                    )
                    .controller(TickBoxController::new)
                    .listener((opt, pendingVal) -> {
                        prioritizeTooltipTopOpt.setAvailable(!pendingVal);
                        bedrockCenteringOpt.setAvailable(!pendingVal);
                        if (pendingVal) {
                            prioritizeTooltipTopOpt.requestSet(false);
                            bedrockCenteringOpt.requestSet(false);
                        }
                    })
                    .build();
            var alignToCornerOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.align_to_corner.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.align_to_corner.desc"))
                    .binding(
                            defaults.bestCorner,
                            () -> config.bestCorner,
                            val -> config.bestCorner = val
                    )
                    .controller(TickBoxController::new)
                    .listener((opt, pendingVal) -> {
                        alwaysAlignToCornerOpt.setAvailable(pendingVal);
                        if (!pendingVal)
                            alwaysAlignToCornerOpt.requestSet(false);
                    })
                    .build();
            var preventVanillaClampingOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.prevent_vanilla_clamping.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.prevent_vanilla_clamping.desc"))
                    .binding(
                            defaults.preventVanillaClamping,
                            () -> config.preventVanillaClamping,
                            val -> config.preventVanillaClamping = val
                    )
                    .controller(TickBoxController::new)
                    .listener((opt, val) -> {
                        bedrockCenteringOpt.setAvailable(val);
                        if (!val) bedrockCenteringOpt.requestSet(false);
                    })
                    .build();
            var applyTweaksToAllPositioners = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.only_reposition_hover_tooltips.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.only_reposition_hover_tooltips.desc"))
                    .binding(
                            defaults.onlyRepositionHoverTooltips,
                            () -> config.onlyRepositionHoverTooltips,
                            val -> config.onlyRepositionHoverTooltips = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            var useYACLTooltipPositionerOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.use_yacl_tooltip_positioner.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.use_yacl_tooltip_positioner.desc"))
                    .binding(
                            defaults.useYACLTooltipPositioner,
                            () -> config.useYACLTooltipPositioner,
                            val -> config.useYACLTooltipPositioner = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            positioningGroup.option(prioritizeTooltipTopOpt);
            positioningGroup.option(bedrockCenteringOpt);
            positioningGroup.option(alignToCornerOpt);
            positioningGroup.option(alwaysAlignToCornerOpt);
            positioningGroup.option(preventVanillaClampingOpt);
            positioningGroup.option(applyTweaksToAllPositioners);
            positioningGroup.option(useYACLTooltipPositionerOpt);
            categoryBuilder.group(positioningGroup.build());

            var scrollingGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("adaptivetooltips.group.scrolling.title"))
                    .tooltip(Component.translatable("adaptivetooltips.group.scrolling.desc"));
            var scrollingInstructions = Option.createBuilder(Component.class)
                    .binding(Binding.immutable(Component.translatable("adaptivetooltips.label.scrolling_instructions", KeyCodeController.DEFAULT_FORMATTER.apply(config.scrollKeyCode), KeyCodeController.DEFAULT_FORMATTER.apply(config.horizontalScrollKeyCode))))
                    .controller(LabelController::new)
                    .build();
            var scrollKeyOpt = Option.createBuilder(int.class)
                    .name(Component.translatable("adaptivetooltips.bind.scroll"))
                    .binding(
                            defaults.scrollKeyCode,
                            () -> config.scrollKeyCode,
                            val -> config.scrollKeyCode = val
                    )
                    .controller(KeyCodeController::new)
                    .build();
            var horizontalScrollKeyOpt = Option.createBuilder(int.class)
                    .name(Component.translatable("adaptivetooltips.bind.horizontal_scroll"))
                    .binding(
                            defaults.horizontalScrollKeyCode,
                            () -> config.horizontalScrollKeyCode,
                            val -> config.horizontalScrollKeyCode = val
                    )
                    .controller(KeyCodeController::new)
                    .build();
            var smoothScrollingOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.smooth_scrolling.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.smooth_scrolling.desc"))
                    .binding(
                            defaults.smoothScrolling,
                            () -> config.smoothScrolling,
                            val -> config.smoothScrolling = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            var scrollDirectionOpt = Option.createBuilder(ScrollDirection.class)
                    .name(Component.translatable("adaptivetooltips.opt.scroll_direction.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.scroll_direction.desc"))
                    .binding(
                            defaults.scrollDirection,
                            () -> config.scrollDirection,
                            val -> config.scrollDirection = val
                    )
                    .controller(EnumController::new)
                    .build();
            var verticalScrollSensOpt = Option.createBuilder(int.class)
                    .name(Component.translatable("adaptivetooltips.opt.vertical_scroll_sensitivity.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.vertical_scroll_sensitivity.desc"))
                    .binding(
                            defaults.verticalScrollSensitivity,
                            () -> config.verticalScrollSensitivity,
                            val -> config.verticalScrollSensitivity = val
                    )
                    .controller(opt -> new IntegerSliderController(opt, 5, 20, 1, val -> Component.translatable("adaptivetooltips.format.pixels", val)))
                    .build();
            var horizontalScrollSensOpt = Option.createBuilder(int.class)
                    .name(Component.translatable("adaptivetooltips.opt.horizontal_scroll_sensitivity.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.horizontal_scroll_sensitivity.desc"))
                    .binding(
                            defaults.horizontalScrollSensitivity,
                            () -> config.horizontalScrollSensitivity,
                            val -> config.horizontalScrollSensitivity = val
                    )
                    .controller(opt -> new IntegerSliderController(opt, 5, 20, 1, val -> Component.translatable("adaptivetooltips.format.pixels", val)))
                    .build();
            scrollingGroup.option(scrollingInstructions);
            scrollingGroup.option(scrollKeyOpt);
            scrollingGroup.option(horizontalScrollKeyOpt);
            scrollingGroup.option(smoothScrollingOpt);
            scrollingGroup.option(scrollDirectionOpt);
            scrollingGroup.option(verticalScrollSensOpt);
            scrollingGroup.option(horizontalScrollSensOpt);
            categoryBuilder.group(scrollingGroup.build());

            var styleGroup = OptionGroup.createBuilder()
                    .name(Component.translatable("adaptivetooltips.group.style.title"))
                    .tooltip(Component.translatable("adaptivetooltips.group.style.desc"));
            var scissorIsSizeOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.scissor_is_size.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.scissor_is_size.desc"))
                    .binding(
                            defaults.scissorIsSize,
                            () -> config.scissorIsSize,
                            val -> config.scissorIsSize = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            var scissorTooltipsOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.scissor_tooltips.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.scissor_tooltips.desc"))
                    .binding(
                            defaults.scissorTooltips,
                            () -> config.scissorTooltips,
                            val -> config.scissorTooltips = val
                    )
                    .controller(TickBoxController::new)
                    .listener((opt, val) -> scissorIsSizeOpt.setAvailable(val))
                    .build();
            var clampHeightOpt = Option.createBuilder(int.class)
                    .name(Component.translatable("adaptivetooltips.opt.clamp_height.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.clamp_height.desc"))
                    .binding(
                            defaults.clampHeight,
                            () -> config.clampHeight,
                            val -> config.clampHeight = val
                    )
                    .controller(opt -> new IntegerSliderController(opt, 10, 100, 1, val -> Component.literal(String.format("%,d%%", val))))
                    .build();
            var tooltipTransparencyOpt = Option.createBuilder(float.class)
                    .name(Component.translatable("adaptivetooltips.opt.tooltip_transparency.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.tooltip_transparency.desc"))
                    .binding(
                            defaults.tooltipTransparency,
                            () -> config.tooltipTransparency,
                            val -> config.tooltipTransparency = val
                    )
                    .controller(opt -> new FloatSliderController(opt, 0f, 1.5f, 0.05f, val -> val == 1f ? Component.translatable("adaptivetooltips.format.vanilla") : Component.literal(String.format("%+,.0f%%", (val - 1) * 100))))
                    .build();
            var removeFirstLinePaddingOpt = Option.createBuilder(boolean.class)
                    .name(Component.translatable("adaptivetooltips.opt.remove_first_line_padding.title"))
                    .tooltip(Component.translatable("adaptivetooltips.opt.remove_first_line_padding.desc"))
                    .binding(
                            defaults.removeFirstLinePadding,
                            () -> config.removeFirstLinePadding,
                            val -> config.removeFirstLinePadding = val
                    )
                    .controller(TickBoxController::new)
                    .build();
            styleGroup.option(scissorTooltipsOpt);
            styleGroup.option(scissorIsSizeOpt);
            styleGroup.option(clampHeightOpt);
            styleGroup.option(tooltipTransparencyOpt);
            styleGroup.option(removeFirstLinePaddingOpt);
            categoryBuilder.group(styleGroup.build());

            return builder
                    .title(Component.translatable("adaptivetooltips.title"))
                    .category(categoryBuilder.build());
        }).generateScreen(parent);

    }
}
