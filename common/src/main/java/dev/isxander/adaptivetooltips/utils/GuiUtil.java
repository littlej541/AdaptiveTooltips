package dev.isxander.adaptivetooltips.utils;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import net.minecraft.util.Mth;

import java.awt.Color;

public class GuiUtil {
    public static int scaleAlpha(int color) {
        Color colorObject = new Color(color, true);
        return new Color(
                colorObject.getRed(),
                colorObject.getGreen(),
                colorObject.getBlue(),
                (int) Mth.clamp(
                        colorObject.getAlpha() * AdaptiveTooltipConfig.INSTANCE.getConfig().tooltipTransparency,
                        0, 255)
        ).getRGB();
    }
}
