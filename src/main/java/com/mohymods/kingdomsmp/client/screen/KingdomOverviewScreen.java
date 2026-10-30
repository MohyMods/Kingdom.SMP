package com.mohymods.kingdomsmp.client.screen;

import com.mohymods.kingdomsmp.client.ClientOverviewController;
import com.mohymods.kingdomsmp.client.ui.*;
import com.mohymods.kingdomsmp.network.KingdomSnapshotPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** A read-only server snapshot. Every control is navigation, inspection, refresh, or close. */
public final class KingdomOverviewScreen extends Screen {
    private KingdomSnapshotPayload snapshot;
    private RoyalLayout layout;
    private boolean progression;
    private int selectedTier;
    private int scroll;
    private int maxScroll;
    private boolean smallWindow;
    private RoyalButton refresh;

    public KingdomOverviewScreen(KingdomSnapshotPayload snapshot) {
        super(Component.translatable("kingdomsmp.screen.title"));
        this.snapshot = snapshot;
        selectedTier = snapshot.tier();
    }

    public void acceptSnapshot(KingdomSnapshotPayload updated) {
        // The connection/session controller limits responses to an outstanding view request.
        snapshot = updated;
        if (minecraft != null) rebuild();
    }

    @Override
    protected void init() {
        smallWindow = width < 300 || height < 220;
        if (smallWindow) {
            addRenderableWidget(new RoyalButton(Math.max(0, width / 2 - 40), Math.max(0, height - 26),
                    80, 20, text("close"), button -> onClose(), text("close.help")));
            return;
        }
        layout = RoyalLayout.fit(width, height);
        int tabWidth = Math.min(128, (layout.contentWidth() - 8) / 2);
        addRenderableWidget(new RoyalTab(layout.contentX(), layout.y() + 48, tabWidth,
                text("overview"), !progression, button -> selectTab(false), text("overview.help")));
        addRenderableWidget(new RoyalTab(layout.contentX() + tabWidth + 8, layout.y() + 48, tabWidth,
                text("progression"), progression, button -> selectTab(true), text("progression.help")));
        if (progression) {
            for (int tier = 1; tier <= 12; tier++) {
                final int selected = tier;
                addRenderableWidget(new RoyalButton(layout.tierX(tier), layout.tierY(tier),
                        layout.tierWidth(), 22, Component.translatable("kingdomsmp.screen.tier.button", tier, stateMarker(tier)),
                        button -> selectTier(selected), tierTooltip(tier)).selected(tier == selectedTier));
            }
        }
        refresh = addRenderableWidget(new RoyalButton(layout.contentX(), layout.footerY(), 100, 20,
                text("refresh"), button -> ClientOverviewController.request(), text("refresh.help")));
        addRenderableWidget(new RoyalButton(layout.x() + layout.width() - 94, layout.footerY(), 80, 20,
                text("close"), button -> onClose(), text("close.help")));
    }

    private void selectTab(boolean progression) {
        this.progression = progression;
        scroll = 0;
        rebuild();
    }

    private void selectTier(int tier) {
        selectedTier = tier;
        scroll = 0;
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, RoyalPalette.BACKDROP);
        if (smallWindow) {
            graphics.drawCenteredString(font, text("small"), width / 2, Math.max(8, height / 2 - 12), RoyalPalette.TEXT);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        RoyalPanel.frame(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        graphics.drawString(font, title, layout.contentX(), layout.y() + 13, RoyalPalette.GOLD, false);
        Component status = Component.translatable("kingdomsmp.screen.tier.status", snapshot.tier());
        graphics.drawString(font, status, layout.contentX(), layout.y() + 29, RoyalPalette.TEXT, false);
        if (layout.width() >= 440) {
            Component revision = Component.translatable("kingdomsmp.screen.revision", snapshot.revision());
            graphics.drawString(font, revision, layout.x() + layout.width() - font.width(revision) - 14,
                    layout.y() + 29, RoyalPalette.MUTED, false);
        }
        if (progression) renderTierDetail(graphics);
        else renderOverview(graphics);
        refresh.active = !ClientOverviewController.waiting();
        refresh.setMessage(text(ClientOverviewController.waiting() ? "refresh.waiting" : "refresh"));
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderOverview(GuiGraphics graphics) {
        int x = layout.contentX();
        int y = layout.bodyY();
        Component tierProgress = Component.translatable("kingdomsmp.screen.tier.progress", snapshot.tier());
        graphics.drawString(font, tierProgress, x, y, RoyalPalette.TEXT, false);
        RoyalProgress.render(graphics, x, y + 14, layout.contentWidth(), 8, snapshot.tier() / 12.0);
        List<Component> paragraphs = new ArrayList<>();
        if (!ClientOverviewController.status().getString().isEmpty()) paragraphs.add(ClientOverviewController.status());
        paragraphs.addAll(List.of(
                text("overview.detail"),
                Component.translatable("kingdomsmp.screen.shared.progress", snapshot.logs(), snapshot.cobblestone(),
                        snapshot.wheat(), snapshot.ironIngots()),
                Component.translatable("kingdomsmp.screen.personal.score", snapshot.personalScoreUnits()),
                text("gates"),
                text("contributions.unavailable")));
        renderParagraphs(graphics, paragraphs, x, y + 30, layout.contentWidth(), layout.bodyHeight() - 30);
    }

    private void renderTierDetail(GuiGraphics graphics) {
        int y = layout.bodyY() + 56;
        int height = layout.bodyHeight() - 56;
        RoyalPanel.parchment(graphics, layout.contentX(), y, layout.contentWidth(), height);
        Component state = Component.translatable("kingdomsmp.screen.selected", selectedTier, state(selectedTier));
        List<Component> paragraphs = new ArrayList<>();
        if (!ClientOverviewController.status().getString().isEmpty()) paragraphs.add(ClientOverviewController.status());
        paragraphs.addAll(List.of(state, tierTitle(selectedTier), gate(selectedTier)));
        if (selectedTier == 1) paragraphs.add(text("tier.first_hearth.targets"));
        paragraphs.add(text("contributions.unavailable"));
        renderParagraphs(graphics, paragraphs,
                layout.contentX() + 6, y + 5, layout.contentWidth() - 12, height - 10);
    }

    private void renderParagraphs(GuiGraphics graphics, List<Component> paragraphs, int x, int y, int width, int height) {
        if (height <= 0) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component paragraph : paragraphs) {
            lines.addAll(font.split(paragraph, width - 8));
            lines.add(FormattedCharSequence.EMPTY);
        }
        maxScroll = Math.max(0, lines.size() * 12 - height);
        scroll = Math.clamp(scroll, 0, maxScroll);
        graphics.enableScissor(x, y, x + width, y + height);
        for (int i = 0; i < lines.size(); i++) {
            int lineY = y + i * 12 - scroll;
            if (lineY + 9 >= y && lineY < y + height) {
                graphics.drawString(font, lines.get(i), x, lineY,
                        progression ? RoyalPalette.INK : RoyalPalette.TEXT, false);
            }
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int barHeight = Math.max(6, height * height / (height + maxScroll));
            int barY = y + (height - barHeight) * scroll / maxScroll;
            graphics.fill(x + width - 2, barY, x + width, barY + barHeight,
                    progression ? RoyalPalette.BRONZE : RoyalPalette.GOLD);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!smallWindow && mouseX >= layout.contentX() && mouseX <= layout.contentX() + layout.contentWidth()
                && mouseY >= layout.bodyY() && mouseY <= layout.bodyY() + layout.bodyHeight()) {
            scroll = Math.clamp(scroll - (int) (vertical * 18), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            scroll = Math.clamp(scroll + (keyCode == GLFW.GLFW_KEY_PAGE_DOWN ? 36 : -36), 0, maxScroll);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Component stateMarker(int tier) {
        return Component.literal(tier < snapshot.tier() ? "+" : tier == snapshot.tier() ? "*" : "-");
    }

    private Component state(int tier) {
        return text(tier < snapshot.tier() ? "state.attained" : tier == snapshot.tier() ? "state.current" : "state.locked");
    }

    private Component tierTitle(int tier) {
        return text(switch (tier) {
            case 1 -> "tier.first_hearth";
            case 2 -> "tier.roads_commerce";
            default -> "tier.future";
        });
    }

    private Component gate(int tier) {
        return text(switch (tier) {
            case 1 -> "gate.town_hall";
            case 5 -> "gate.nether";
            case 9 -> "gate.end";
            case 12 -> "gate.cube";
            default -> "gate.unspecified";
        });
    }

    private Component tierTooltip(int tier) {
        return Component.translatable("kingdomsmp.screen.selected", tier, state(tier))
                .append(Component.literal("\n")).append(gate(tier));
    }

    private static Component text(String key) {
        return Component.translatable("kingdomsmp.screen." + key);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
