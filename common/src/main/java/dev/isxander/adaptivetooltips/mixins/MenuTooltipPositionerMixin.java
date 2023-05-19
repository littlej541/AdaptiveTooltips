package dev.isxander.adaptivetooltips.mixins;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import net.minecraft.client.gui.screens.inventory.tooltip.MenuTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(MenuTooltipPositioner.class)
public class MenuTooltipPositionerMixin {
    @ModifyArg(method = "positionTooltip", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"), index = 1)
    private int preventVanillaClamping(int max) {
        // setting the minimum x value to be Integer.MIN_VALUE essentially preventing clamping
        return AdaptiveTooltipConfig.INSTANCE.getConfig().preventVanillaClamping ? Integer.MIN_VALUE : max;
    }
}
