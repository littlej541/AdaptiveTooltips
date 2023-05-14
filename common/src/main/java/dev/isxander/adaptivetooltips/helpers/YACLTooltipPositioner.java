package dev.isxander.adaptivetooltips.helpers;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.util.Mth;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class YACLTooltipPositioner implements ClientTooltipPositioner {
    private final AbstractWidget clickableWidget;

    public YACLTooltipPositioner(AbstractWidget clickableWidget) {
        this.clickableWidget = clickableWidget;
    }

    @Override
    public Vector2ic positionTooltip(Screen screen, int x, int y, int width, int height) {
        int centerX = clickableWidget.getX() + clickableWidget.getWidth() / 2;
        int aboveY = clickableWidget.getY() - height - 4;
        int belowY = clickableWidget.getY() + clickableWidget.getHeight() + 4;

        int maxBelow = screen.height - (belowY + height);
        int minAbove = aboveY - height;

        int yResult = aboveY;
        if (minAbove < 8)
            yResult = maxBelow > minAbove ? belowY : aboveY;

        int xResult = Mth.clamp(centerX - width / 2, -4, screen.width - width - 4);

        return new Vector2i(xResult, yResult);
    }
}
