package dev.isxander.adaptivetooltips.helpers;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import net.minecraft.util.Mth;

public class TooltipData {
    private final Tooltip tooltip;
    private final Viewport viewport;

    private TooltipData(Tooltip tooltip, Viewport viewport) {
        this.tooltip = tooltip.copy();
        this.viewport = viewport.copy();
    }

    private TooltipData(TooltipData data) {
        this.tooltip = data.tooltip;
        this.viewport = data.viewport;
    }

    public static TooltipData create(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        return new TooltipData(Tooltip.create(x, y, width, height), Viewport.create(x, y, width, height, screenWidth, screenHeight, 0));
    }

    public TooltipData withPosition(int x, int y, int screenWidth, int screenHeight) {
        return new TooltipData(this.tooltip.withPosition(x, y), this.viewport.withPosition(x, y, screenWidth, screenHeight));
    }

    public TooltipData withSize(int width, int height, int screenWidth, int screenHeight) {
        return new TooltipData(this.tooltip.withSize(width, height), this.viewport.withSize(width, height, screenWidth, screenHeight));
    }

    public TooltipData copy() {
        return new TooltipData(this);
    }

    public int x() {
        return this.tooltip.x;
    }

    public int y() {
        return this.tooltip.y;
    }

    public int width() {
        return treatAsSize() ? this.viewport.width : this.tooltip.width;
    }

    public int height() {
        return treatAsSize() ? this.viewport.height : this.tooltip.height;
    }

    public Tooltip tooltip() {
        return this.tooltip;
    }

    public Viewport viewport() {
        return this.viewport;
    }

    public static int calculateHeightClamp(int screenHeight) {
        if (!AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips)
            return Integer.MAX_VALUE;

        return (int) ((float) AdaptiveTooltipConfig.INSTANCE.getConfig().clampHeight / 100 * screenHeight);
    }

    private static boolean treatAsSize() {
        return AdaptiveTooltipConfig.INSTANCE.getConfig().scissorTooltips && AdaptiveTooltipConfig.INSTANCE.getConfig().scissorIsSize;
    }

    public static class Tooltip {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        private Tooltip(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private Tooltip(Tooltip tooltip) {
            this(tooltip.x, tooltip.y, tooltip.width, tooltip.height);
        }

        private static Tooltip create(int x, int y, int width, int height) {
            return new Tooltip(x, y, width, height);
        }

        public Tooltip withPosition(int x, int y) {
            return create(x, y, this.width, this.height);
        }

        public Tooltip withSize(int width, int height) {
            return create(this.x, this.y, width, height);
        }

        public Tooltip copy() {
            return new Tooltip(this);
        }
    }

    public static class Viewport {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        private Viewport(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        private Viewport(Viewport viewport) {
            this(viewport.x, viewport.y, viewport.width, viewport.height);
        }

        private static Viewport create(int x, int y, int width, int height, int screenWidth, int screenHeight, int edgeOffset) {
            int newX = Mth.clamp(x, edgeOffset, screenWidth - edgeOffset);
            int newY = Mth.clamp(y, edgeOffset, screenHeight - edgeOffset);

            int newWidth = Math.min(width - newX + x, screenWidth - newX - edgeOffset);
            int newHeight = Math.min(Math.min(height - newY + y, screenHeight - newY - edgeOffset), calculateHeightClamp(screenHeight));

            return new Viewport(newX, newY, newWidth, newHeight);
        }

        public Viewport withPosition(int x, int y, int screenWidth, int screenHeight) {
            Viewport newViewport = create(x, y, this.width, this.height, screenWidth, screenHeight, 0);
            return new Viewport(newViewport.x, newViewport.y, this.width, this.height);
        }

        public Viewport withSize(int width, int height, int screenWidth, int screenHeight) {
            return create(this.x, this.y, width, height, screenWidth, screenHeight, 0);
        }

        public Viewport withOffset(int edgeOffset, int screenWidth, int screenHeight) {
            return create(this.x, this.y, this.width, this.height, screenWidth, screenHeight, edgeOffset);
        }

        public Viewport copy() {
            return new Viewport(this);
        }
    }
}
