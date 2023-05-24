package dev.isxander.adaptivetooltips.mixins.forge;

import dev.isxander.adaptivetooltips.helpers.TooltipWrapper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Mixin(ForgeHooksClient.class)
public class ForgeHooksClientMixin {
    @Unique private static RenderTooltipEvent.GatherComponents capturedEvent;

    @ModifyVariable(method = "gatherTooltipComponents(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/Optional;IIILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/Font;)Ljava/util/List;", at = @At(value = "STORE", ordinal = 0), ordinal =  0, remap = false)
    private static RenderTooltipEvent.GatherComponents captureEvent(RenderTooltipEvent.GatherComponents event) {
        capturedEvent = event;
        return event;
    }

    @Inject(method = "gatherTooltipComponents(Lnet/minecraft/world/item/ItemStack;Ljava/util/List;Ljava/util/Optional;IIILnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/Font;)Ljava/util/List;", at = @At(value = "INVOKE_ASSIGN", target = "Ljava/util/OptionalInt;orElse(I)I"), cancellable = true, remap = false)
    private static void interceptForgeHook(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent, int mouseX, int screenWidth, int screenHeight, @Nullable Font forcedFont, Font fallbackFont, CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        Font font = ForgeHooksClient.getTooltipFont(forcedFont, stack, fallbackFont);
        int maxWidth = capturedEvent.getMaxWidth();

        List<ClientTooltipComponent> lines = capturedEvent.getTooltipElements().stream()
                .flatMap(either -> either.map(
                        text -> TooltipWrapper.wrapTooltipLines(font, List.of(text), mouseX, maxWidth, screenWidth).stream().map(ClientTooltipComponent::create),
                        component -> Stream.of(ClientTooltipComponent.create(component))))
                .toList();

        cir.setReturnValue(lines);
    }
}
