package io.github.naimjeg.combatnexus.presentation.platform.client;

import com.google.gson.JsonArray;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Scrollable read-only projection; numeric values are supplied by the server. */
public final class CombatPanelScreen extends Screen {
    private List<Component> rows = List.of(Component.translatable("gui.combatnexus.panel.loading"));
    private int scroll;
    public List<Component> rows() { return rows; }
    CombatPanelScreen() { super(Component.translatable("gui.combatnexus.panel.title")); }
    void invalidate() { rows = List.of(Component.translatable("gui.combatnexus.panel.loading")); scroll = 0; }
    void accept(JsonArray values) {
        if (values == null || values.size() > 512) return;
        List<Component> next = new ArrayList<>();
        for (var value : values) {
            if (value.isJsonPrimitive()) { next.add(Component.literal(value.getAsString())); continue; }
            var row = value.getAsJsonObject();
            if(row.has("display")) {
                var display=io.github.naimjeg.combatnexus.presentation.api.DisplayText.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,row.get("display")).getOrThrow();
                next.add(switch(display) {
                    case io.github.naimjeg.combatnexus.presentation.api.DisplayText.Literal literal -> Component.literal(literal.text());
                    case io.github.naimjeg.combatnexus.presentation.api.DisplayText.Translatable translated -> Component.translatableWithFallback(translated.key(),translated.fallback().orElse(null),translated.args().toArray());
                });continue;
            }
            if (row.has("message")) { next.add(Component.translatable(row.get("message").getAsString())); continue; }
            if (row.has("health")) {
                next.add(Component.translatable("gui.combatnexus.panel.health", number(row.get("health").getAsDouble()),
                        number(row.get("absorption").getAsDouble())));
                continue;
            }
            String unit = row.get("unit").getAsString();
            boolean percentage = unit.equals("PROBABILITY") || unit.equals("ADDITIVE_RATIO") || unit.equals("CONVERSION_RATIO");
            double scale = percentage ? 100 : 1;
            String suffix = percentage ? "%" : "";
            var line = Component.translatable(row.get("name").getAsString()).append("  "
                    + number(row.get("value").getAsDouble() * scale) + suffix + "  ");
            if (!unit.isEmpty()) line.append(Component.translatable("gui.combatnexus.unit." + unit.toLowerCase(Locale.ROOT))).append("  ");
            line.append(Component.translatable("gui.combatnexus.panel.base", number(row.get("base").getAsDouble() * scale) + suffix));
            if(row.has("raw"))line.append("  ").append(Component.translatable("gui.combatnexus.panel.effective_reason",number(row.get("raw").getAsDouble()*scale)+suffix,row.get("effective_reason").getAsString()));
            if (row.has("reserved") && row.get("reserved").getAsBoolean())
                line.append("  ").append(Component.translatable("gui.combatnexus.panel.reserved"));
            next.add(line);
        }
        rows = List.copyOf(next);
        scroll = Math.min(scroll, Math.max(0, rows.size() - visible()));
    }
    private static String number(double value) { return io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers.number(value); }
    private int visible() { return Math.max(1, (height - 68) / 12); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.clamp(scroll - (int) Math.signum(dy) * 3, 0, Math.max(0, rows.size() - visible()));
        return true;
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(8, 8, width - 8, height - 8, 0xEE141B24);
        graphics.text(font, title, 20, 20, 0xFFFFFFFF, false);
        graphics.enableScissor(18, 40, width - 18, height - 24);
        for (int index = scroll; index < Math.min(rows.size(), scroll + visible()); index++) {
            graphics.text(font, rows.get(index), 20, 42 + (index - scroll) * 12, 0xFFD6DFEA, false);
        }
        graphics.disableScissor();
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
}
