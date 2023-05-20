package dev.isxander.adaptivetooltips.helpers.positioner;

import dev.isxander.adaptivetooltips.helpers.TooltipData;

public interface TooltipPositionModule {
    TooltipData repositionTooltip(TooltipData tooltipData, int mouseX, int mouseY, int screenWidth, int screenHeight);
}
