package dev.isxander.adaptivetooltips.helpers;

import dev.isxander.adaptivetooltips.config.AdaptiveTooltipConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TooltipWrapper {
    public static List<FormattedCharSequence> wrapTooltipLines(Screen screen, Font textRenderer, List<? extends Component> lines, int x, ClientTooltipPositioner tooltipPositioner) {
        if (lines.stream().allMatch(text -> text.getString().isBlank()))
            return List.of();
        int maxWidth = getMaxWidth(textRenderer, lines);

        int allowedMaxWidth = 0;
        switch (AdaptiveTooltipConfig.INSTANCE.getConfig().wrapText) {
            case OFF ->
                    allowedMaxWidth = Integer.MAX_VALUE; // max_value essentially bypasses wrapping using the check below
            case SCREEN_WIDTH ->
                    allowedMaxWidth = screen.width - 15; // minus 15 to add a bit of padding to each side
            case REMAINING_WIDTH -> {
                // can't rely on tooltip x as wrapping is calculated *before* tooltip positioners
                // any we can't use tooltip positioners early without height, which you get by wrapping! infinite loop!
                if (tooltipPositioner instanceof DefaultTooltipPositioner) {
                    allowedMaxWidth = screen.width - x - 15;

                    if (x + 12 + maxWidth > screen.width)
                        allowedMaxWidth = Math.max(allowedMaxWidth, x - 20);
                } else {
                    allowedMaxWidth = Integer.MAX_VALUE; // max_value essentially bypasses wrapping using the check below
                }
            }
            case HALF_SCREEN_WIDTH ->
                allowedMaxWidth = screen.width / 2;
            case SMART -> {
                if (lines.size() <= 1) { // calculating diffs only works with 2 or more
                    allowedMaxWidth = screen.width / 4 * 3;
                } else {
                    AtomicInteger idx = new AtomicInteger(0);
                    // map each line to its width and group it with its index for later use
                    Map<Integer, Integer> widths = lines.stream()
                            .map(textRenderer::width)
                            .map(width -> new AbstractMap.SimpleEntry<>(idx.getAndIncrement(), width))
                            .filter(entry -> entry.getValue() > 0)
                            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

                    // get the difference between each value, retaining the index
                    List<Map.Entry<Integer, Integer>> diffs = new ArrayList<>();
                    Iterator<Map.Entry<Integer, Integer>> iterator = widths.entrySet().iterator();
                    int width = iterator.next().getValue();
                    while (iterator.hasNext()) {
                        Map.Entry<Integer, Integer> entry = iterator.next();
                        int diff = entry.getValue() - width;
                        width = entry.getValue();
                        diffs.add(new AbstractMap.SimpleEntry<>(entry.getKey(), diff));
                    }

                    // find the largest increase below 100px then get the index
                    Optional<Integer> index = diffs.stream()
                            .filter(entry -> entry.getValue() <= 100)
                            .sorted(Comparator.comparingInt(entry -> -entry.getValue()))
                            .map(Map.Entry::getKey)
                            .findFirst();

                    // if no matches, just wrap half of 3/4 screen width
                    allowedMaxWidth = index.map(integer -> Math.min(textRenderer.width(lines.get(integer)), screen.width / 4 * 3)).orElse(screen.width / 4 * 3);
                }
            }
        }

        // if max width is less than allowed max width, there is no need to do any wrapping
        if (maxWidth <= allowedMaxWidth)
            return lines.stream().map(Component::getVisualOrderText).collect(Collectors.toList());

        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            wrapped.addAll(textRenderer.split(line, allowedMaxWidth));
        }

        return wrapped;
    }

    private static int getMaxWidth(Font textRenderer, List<? extends Component> lines) {
        int maxWidth = 0;

        for (Component line : lines) {
            int width = textRenderer.width(line);
            if (width > maxWidth)
                maxWidth = width;
        }

        return maxWidth;
    }
}
