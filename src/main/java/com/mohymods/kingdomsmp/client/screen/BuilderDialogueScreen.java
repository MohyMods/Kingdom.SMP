package com.mohymods.kingdomsmp.client.screen;

import com.mohymods.kingdomsmp.client.BuilderDialogueController;
import com.mohymods.kingdomsmp.client.ui.RoyalButton;
import com.mohymods.kingdomsmp.client.ui.RoyalLayout;
import com.mohymods.kingdomsmp.client.ui.RoyalPalette;
import com.mohymods.kingdomsmp.client.ui.RoyalPanel;
import com.mohymods.kingdomsmp.network.BuilderDialoguePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Server-backed builder conversation and catalogue inspection. No world mutations. */
public final class BuilderDialogueScreen extends Screen {
    private static final String[] TOPICS = {"Blueprints", "Current work", "Build time", "About you"};
    private BuilderDialoguePayload snapshot;
    private RoyalLayout layout;
    private boolean smallWindow;
    private int readingY;
    private int readingHeight;
    private int scroll;
    private int maxScroll;
    private final List<RoyalButton> topics = new ArrayList<>();
    private RoyalButton back;

    public BuilderDialogueScreen(BuilderDialoguePayload snapshot) {
        super(text("Builder's Hut"));
        this.snapshot = snapshot;
    }

    public UUID npcId() { return snapshot.npcId(); }

    public void acceptSnapshot(BuilderDialoguePayload updated) {
        if (!snapshot.npcId().equals(updated.npcId())) return;
        if (snapshot.topic() != updated.topic()) scroll = 0;
        snapshot = updated;
        if (minecraft != null) rebuild();
    }

    @Override
    protected void init() {
        topics.clear();
        smallWindow = width < 300 || height < 220;
        if (smallWindow) {
            addRenderableWidget(new RoyalButton(Math.max(0, width / 2 - 40), Math.max(0, height - 26),
                    80, 20, text("Leave"), button -> onClose(), text("End this conversation.")));
            return;
        }
        layout = RoyalLayout.fit(width, height);
        int columns = layout.contentWidth() >= 480 ? 4 : 2;
        int choiceWidth = (layout.contentWidth() - (columns - 1) * 6) / columns;
        int choicesY = layout.y() + 80;
        for (int index = 0; index < TOPICS.length; index++) {
            int topic = index + 1;
            RoyalButton choice = new RoyalButton(layout.contentX() + (index % columns) * (choiceWidth + 6),
                    choicesY + (index / columns) * 24, choiceWidth, 20, text(TOPICS[index]),
                    button -> BuilderDialogueController.request(topic),
                    text("Ask the builder: " + TOPICS[index].toLowerCase() + "."));
            choice.selected(snapshot.topic() == topic);
            topics.add(addRenderableWidget(choice));
        }
        readingY = choicesY + ((TOPICS.length + columns - 1) / columns) * 24 + 5;
        readingHeight = layout.footerY() - 10 - readingY;
        back = addRenderableWidget(new RoyalButton(layout.contentX(), layout.footerY(), 72, 20,
                text("Back"), button -> BuilderDialogueController.request(0), text("Return to the greeting.")));
        addRenderableWidget(new RoyalButton(layout.x() + layout.width() - 90, layout.footerY(), 76, 20,
                text("Leave"), button -> onClose(), text("End this conversation.")));
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The Royal Gilded backdrop is rendered below; avoid vanilla's later blur pass.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, RoyalPalette.BACKDROP);
        if (smallWindow) {
            graphics.drawCenteredString(font, text("Increase the window size to speak with the builder."),
                    width / 2, Math.max(8, height / 2 - 12), RoyalPalette.TEXT);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        RoyalPanel.frame(graphics, layout.x(), layout.y(), layout.width(), layout.height());
        graphics.drawString(font, title, layout.contentX(), layout.y() + 13, RoyalPalette.GOLD, false);
        graphics.drawString(font, text("KINGDOM SMP"), layout.x() + layout.width() - 86,
                layout.y() + 13, RoyalPalette.MUTED, false);
        renderIdentity(graphics, mouseX, mouseY);
        RoyalPanel.parchment(graphics, layout.contentX(), readingY, layout.contentWidth(), readingHeight);
        renderReading(graphics);
        boolean waiting = BuilderDialogueController.waiting();
        for (RoyalButton topic : topics) topic.active = !waiting;
        back.active = !waiting && snapshot.topic() != 0;
        if (!BuilderDialogueController.status().getString().isEmpty()) {
            int statusX = layout.contentX() + 80;
            int statusWidth = layout.contentWidth() - 164;
            String status = font.plainSubstrByWidth(BuilderDialogueController.status().getString(), statusWidth);
            graphics.drawString(font, status, statusX, layout.footerY() + 6, RoyalPalette.MUTED, false);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderIdentity(GuiGraphics graphics, int mouseX, int mouseY) {
        int portraitX = layout.contentX();
        int portraitY = layout.y() + 31;
        graphics.fill(portraitX, portraitY, portraitX + 44, portraitY + 43, RoyalPalette.BRONZE);
        graphics.fill(portraitX + 1, portraitY + 1, portraitX + 43, portraitY + 42, RoyalPalette.PANEL);
        LivingEntity builder = BuilderDialogueController.localNpc(snapshot.npcId());
        if (builder != null) {
            Component name = builder.getCustomName();
            boolean nameVisible = builder.isCustomNameVisible();
            try {
                // The identity is already printed beside the portrait; a world
                // nameplate would overlap the small model frame.
                builder.setCustomName(null);
                builder.setCustomNameVisible(false);
                InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, portraitX + 2, portraitY + 2,
                        portraitX + 42, portraitY + 41, 17, 0.0625F, mouseX, mouseY, builder);
            } finally {
                builder.setCustomName(name);
                builder.setCustomNameVisible(nameVisible);
            }
        }
        int nameX = portraitX + 55;
        int nameWidth = layout.contentWidth() - 55;
        graphics.drawString(font, font.plainSubstrByWidth(snapshot.name(), nameWidth), nameX,
                portraitY + 4, RoyalPalette.TEXT, false);
        graphics.drawString(font, text("Master Builder"), nameX, portraitY + 18, RoyalPalette.GOLD, false);
        graphics.drawString(font, text("Timber • Stone • Craft"), nameX, portraitY + 32, RoyalPalette.MUTED, false);
    }

    private void renderReading(GuiGraphics graphics) {
        int x = layout.contentX() + 8;
        int y = readingY + 7;
        int textWidth = layout.contentWidth() - 24;
        int textHeight = readingHeight - 14;
        if (textHeight <= 0) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (Component paragraph : paragraphs()) {
            lines.addAll(font.split(paragraph, textWidth));
            lines.add(FormattedCharSequence.EMPTY);
        }
        maxScroll = Math.max(0, lines.size() * 12 - textHeight);
        scroll = Math.clamp(scroll, 0, maxScroll);
        graphics.enableScissor(x, y, x + textWidth, y + textHeight);
        for (int index = 0; index < lines.size(); index++) {
            int lineY = y + index * 12 - scroll;
            if (lineY + 9 >= y && lineY < y + textHeight) {
                graphics.drawString(font, lines.get(index), x, lineY, RoyalPalette.INK, false);
            }
        }
        graphics.disableScissor();
        if (maxScroll > 0) {
            int railX = layout.contentX() + layout.contentWidth() - 7;
            graphics.fill(railX, y, railX + 2, y + textHeight, 0xFFB7A382);
            int thumbHeight = Math.max(8, textHeight * textHeight / (textHeight + maxScroll));
            int thumbY = y + (textHeight - thumbHeight) * scroll / maxScroll;
            graphics.fill(railX - 1, thumbY, railX + 3, thumbY + thumbHeight, RoyalPalette.BRONZE);
        }
    }

    private List<Component> paragraphs() {
        return switch (snapshot.topic()) {
            case 1 -> List.of(text("TOWN HALL BLUEPRINT"),
                    text(snapshot.hearthComplete() ? "First Hearth milestone complete." : "Locked reward • Complete The First Hearth."),
                    text("The highest contributor earns one exclusive Town Hall blueprint. Materials are included; the completed hall belongs to the kingdom."),
                    text("Shared milestone: 1,024 logs • 2,048 cobblestone • 512 wheat • 128 iron ingots."),
                    text("Each category has equal weight. Ties go to the first player to reach the tied score."),
                    text("Town Hall construction is unavailable. The plans can be inspected here while the kingdom prepares."));
            case 2 -> List.of(text("CURRENT WORK"), text(snapshot.status()),
                    text("The hut is my home and workshop. No building project is currently assigned."));
            case 3 -> List.of(text("BUILD TIME"), text(snapshot.status()),
                    text("Good work takes the time it takes. There is no active construction schedule to estimate yet."),
                    text("Ask me again when a building project has been assigned."));
            case 4 -> List.of(text(snapshot.name() + " • MASTER BUILDER"),
                    text("I've spent enough winters setting stone to know that a straight wall begins with a sound foundation."),
                    text("Bring patience, a clear site, and respect for the craft. We'll give this kingdom buildings worth keeping."));
            default -> List.of(text("A SOUND FOUNDATION"),
                    text("Mind the shavings. A kingdom needs more than a grand name; it needs walls that stand and roofs that keep the rain out."),
                    text("I'm " + snapshot.name() + ", your Master Builder. Have a look at the plans, or ask me about the work."),
                    text(snapshot.status()));
        };
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!smallWindow && mouseX >= layout.contentX() && mouseX <= layout.contentX() + layout.contentWidth()
                && mouseY >= readingY && mouseY <= readingY + readingHeight) {
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

    @Override
    public void onClose() {
        BuilderDialogueController.clear();
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() { return false; }
    private static Component text(String value) { return Component.literal(value); }
}
