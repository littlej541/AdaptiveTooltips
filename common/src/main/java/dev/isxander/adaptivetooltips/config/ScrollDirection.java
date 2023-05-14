package dev.isxander.adaptivetooltips.config;

import dev.isxander.yacl.api.NameableEnum;
import net.minecraft.network.chat.Component;

public enum ScrollDirection implements NameableEnum {
    REVERSE,
    NATURAL;

    public Component getDisplayName() {
        return Component.translatable("adaptivetooltips.scroll_direction." + name().toLowerCase());
    }
}
