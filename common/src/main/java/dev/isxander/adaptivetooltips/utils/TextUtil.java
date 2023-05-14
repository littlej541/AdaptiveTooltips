package dev.isxander.adaptivetooltips.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {
    public static MutableComponent toText(FormattedCharSequence orderedText) {
        MutableComponent text = Component.empty();

        // constructs a Text by iterating over each character in OrderedText and appending it with its own style
        orderedText.accept((idx, style, codePoint) -> {
            text.append(Component.literal(Character.toString(codePoint)).setStyle(style));
            return true;
        });

        return text;
    }

    public static List<MutableComponent> toText(Iterable<? extends FormattedCharSequence> orderedTextList) {
        List<MutableComponent> texts = new ArrayList<>();

        for (FormattedCharSequence orderedText : orderedTextList) {
            texts.add(toText(orderedText));
        }

        return texts;
    }
}
