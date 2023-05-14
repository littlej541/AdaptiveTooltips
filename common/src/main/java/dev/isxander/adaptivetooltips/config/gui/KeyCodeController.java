package dev.isxander.adaptivetooltips.config.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.isxander.yacl.api.Controller;
import dev.isxander.yacl.api.Option;
import dev.isxander.yacl.api.utils.Dimension;
import dev.isxander.yacl.gui.AbstractWidget;
import dev.isxander.yacl.gui.YACLScreen;
import dev.isxander.yacl.gui.controllers.ControllerWidget;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

public class KeyCodeController implements Controller<Integer> {
    public static final Function<Integer, Component> DEFAULT_FORMATTER = code -> InputConstants.Type.KEYSYM.getOrCreate(code).getDisplayName();

    private final Option<Integer> option;
    private final Function<Integer, Component> valueFormatter;

    public KeyCodeController(Option<Integer> option) {
        this(option, DEFAULT_FORMATTER);
    }

    public KeyCodeController(Option<Integer> option, Function<Integer, Component> valueFormatter) {
        this.option = option;
        this.valueFormatter = valueFormatter;
    }

    @Override
    public Option<Integer> option() {
        return option;
    }

    @Override
    public Component formatValue() {
        return valueFormatter.apply(option().pendingValue());
    }

    @Override
    public AbstractWidget provideWidget(YACLScreen yaclScreen, Dimension<Integer> dimension) {
        return new KeyCodeControllerElement(this, yaclScreen, dimension);
    }

    public static class KeyCodeControllerElement extends ControllerWidget<KeyCodeController> {
        private boolean awaitingKeyPress = false;

        private KeyCodeControllerElement(KeyCodeController control, YACLScreen screen, Dimension<Integer> dim) {
            super(control, screen, dim);
        }

        @Override
        protected void drawHoveredControl(PoseStack matrices, int mouseX, int mouseY, float delta) {

        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY) || !isAvailable()) {
                return false;
            }

            awaitingKeyPress = !awaitingKeyPress;
            return true;
        }

        @Override
        protected Component getValueText() {
            if (awaitingKeyPress)
                return Component.translatable("adaptivetooltips.gui.awaiting_key").withStyle(ChatFormatting.ITALIC);

            return super.getValueText();
        }

        @Override
        protected int getHoveredControlWidth() {
            return getUnhoveredControlWidth();
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (awaitingKeyPress) {
                control.option().requestSet(keyCode);
                awaitingKeyPress = false;
                return true;
            }

            return false;
        }
    }
}
