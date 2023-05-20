package dev.isxander.adaptivetooltips.helpers.positioner;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.helpers.TooltipData;

public class PrioritizeTooltipTopPositionModule implements TooltipPositionModule {
    @Override
    public TooltipData repositionTooltip(TooltipData tooltipData, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().prioritizeTooltipTop || tooltipData.y() >= 4)
            return tooltipData;

        return tooltipData.withPosition(tooltipData.x(), 4, screenWidth, screenHeight);
    }
}
