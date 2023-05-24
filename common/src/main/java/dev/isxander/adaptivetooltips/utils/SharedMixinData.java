package dev.isxander.adaptivetooltips.utils;

import dev.isxander.adaptivetooltips.helpers.TooltipData;
import org.joml.Matrix4f;

public class SharedMixinData {
    public static TooltipData tooltipData = null;
    public static TooltipData.Viewport edgeOffsetViewport = null;

    public static Matrix4f preScrollTransform;

    public static boolean alreadyWrapped = false;
}
