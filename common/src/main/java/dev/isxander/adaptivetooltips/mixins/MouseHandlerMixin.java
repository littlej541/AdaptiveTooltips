package dev.isxander.adaptivetooltips.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import dev.isxander.adaptivetooltips.helpers.ScrollTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow @Final private Minecraft minecraft;

    @WrapOperation(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/Screen;mouseScrolled(DDD)Z"))
    private boolean trackMouseWheel(Screen screen, double mouseX, double mouseY, double amount, Operation<Boolean> original, long window, double horizontal, double vertical) {
        if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), AdaptiveTooltipConfig.INSTANCE.getConfig().scrollKeyCode)) {
            if (InputConstants.isKeyDown(minecraft.getWindow().getWindow(), AdaptiveTooltipConfig.INSTANCE.getConfig().horizontalScrollKeyCode)) {
                ScrollTracker.addHorizontalScroll((int) Math.signum(vertical));
            } else {
                ScrollTracker.addVerticalScroll((int) Math.signum(vertical));
                ScrollTracker.addHorizontalScroll((int) Math.signum(horizontal));
            }
            return true;
        }

        return original.call(screen, mouseX, mouseY, amount);
    }
}
