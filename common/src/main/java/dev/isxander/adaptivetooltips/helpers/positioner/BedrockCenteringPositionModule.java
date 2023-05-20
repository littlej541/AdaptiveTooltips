package dev.isxander.adaptivetooltips.helpers.positioner;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.helpers.TooltipData;
import net.minecraft.util.Mth;

public class BedrockCenteringPositionModule implements TooltipPositionModule {
    @Override
    public TooltipData repositionTooltip(TooltipData tooltipData, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().bedrockCentering)
            return tooltipData;

        int modX = tooltipData.x();
        int modY = tooltipData.y();

        if (tooltipData.x() < 4) {
            modX = Mth.clamp(mouseX - tooltipData.width() / 2, 6, screenWidth - tooltipData.width() - 6);
            modY = mouseY - tooltipData.height() - 12;

            if (modY < 6) {
                // find amount of obstruction to decide if it
                // is best to be above or below cursor
                var below = mouseY + 12;
                var belowObstruction = below + tooltipData.height() - screenHeight;
                var aboveObstruction = -modY;

                if (belowObstruction < aboveObstruction) {
                    modY = below;
                }
            }
        } else if (tooltipData.y() + tooltipData.height() > screenHeight + 2) {
            modY = Math.max(screenHeight - tooltipData.height() - 4, 4);
        } else {
            // doesn't need to be repositioned
            return tooltipData;
        }

        return tooltipData.withPosition(modX, modY, screenWidth, screenHeight);
    }
}
