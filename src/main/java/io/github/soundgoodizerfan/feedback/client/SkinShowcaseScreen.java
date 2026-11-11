// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.Command;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Dev tool, {@code /guiskins}: one window showing every {@link GuiStyle} widget and {@link GuiSkin}
 * primitive on one skin, with Left/Right (or the arrow buttons) flipping through all of them. Replay
 * re-runs the skin's reveal and sounds. Nothing here is saved or sent.
 */
public final class SkinShowcaseScreen extends Screen {

    private static final int W = 330, H = 232;
    private static final GuiSkin[] SKINS = GuiSkin.values();

    private int index;
    private Opening opening = new Opening(SKINS[0]);

    // demo state, kept across skin flips so a flip shows the same values on a new face
    private boolean toggled = true, checked = true;
    private int steps = 3, tab = 0, folder = 0, picked = 1, scroll = 2;
    private double slid = 6;
    private long stampAt = -10_000;

    private SkinShowcaseScreen() {
        super(Component.literal("GUI skins"));
    }

    private GuiSkin skin() {
        return SKINS[index];
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private void flip(int by) {
        index = Math.floorMod(index + by, SKINS.length);
        replay();
    }

    private void replay() {
        opening = new Opening(skin());
        opening.start();
        rebuildWidgets();
    }

    @Override
    protected void init() {
        opening.start();
        GuiSkin s = skin();
        int x = left() + 12, y = top() + 26, col = 150;

        add(GuiStyle.button(s, Component.literal("Button"), b -> {}, x, y, 70, 14));
        AbstractWidget off = add(GuiStyle.button(s, Component.literal("Disabled"), b -> {}, x + 76, y, 70, 14));
        off.active = false;
        add(GuiStyle.primary(s, Component.literal("Primary"), b -> stampAt = System.currentTimeMillis(),
                x, y + 18, 70, 16, FSounds.UI_STAMP::get));
        add(GuiStyle.button(s, Component.literal("Stage"), b -> s.cue(GuiSkin.Cue.STAGE), x + 76, y + 18, 70, 16));
        add(GuiStyle.toggle(s, Component.literal("Toggle"), () -> toggled, b -> toggled = !toggled, x, y + 40, col));
        add(GuiStyle.checkbox(s, Component.literal("Check box"), () -> checked, b -> checked = !checked, x, y + 58, col));
        add(GuiStyle.stepper(s, x, y + 76, col, 14, "<", ">", () -> "Value " + steps,
                d -> steps = Math.max(0, Math.min(9, steps + d)), false, null));
        add(GuiStyle.stepper(s, x, y + 94, col, 14, "-", "+", () -> "Plaque " + steps,
                d -> steps = Math.max(0, Math.min(9, steps + d)), true, null));
        add(GuiStyle.slider(s, x, y + 114, col, 0, 10, 1, () -> slid, v -> slid = v));
        add(GuiStyle.tabs(s, x, y + 130, col, 14, List.of(Component.literal("One"), Component.literal("Two"),
                Component.literal("Three")), () -> tab, i -> tab = i));
        add(new FolderTabs(s, left() + 8, top() - FolderTabs.HEIGHT, List.of(
                new FolderTabs.Def(Component.literal("Folder"), new ItemStack(Items.PAPER), true),
                new FolderTabs.Def(Component.literal("Tabs")),
                new FolderTabs.Def(Component.literal("Locked"), ItemStack.EMPTY, false)),
                () -> folder, i -> folder = i).host(left(), top(), W, H));

        // cues
        int cx = x, cy = y + 150;
        GuiSkin.Cue[] cues = {GuiSkin.Cue.SEAT, GuiSkin.Cue.REJECT, GuiSkin.Cue.START, GuiSkin.Cue.FINISH, GuiSkin.Cue.ABORT};
        for (int i = 0; i < cues.length; i++) {
            GuiSkin.Cue c = cues[i];
            add(GuiStyle.button(s, Component.literal(c.name().substring(0, 1) + c.name().substring(1).toLowerCase(Locale.ROOT)),
                    b -> s.cue(c), cx + (i % 3) * 50, cy + (i / 3) * 16, 48, 14));
        }

        // navigation
        int ny = top() + H - 24;
        add(GuiStyle.button(s, Component.literal("<"), b -> flip(-1), left() + 12, ny, 24, 14));
        add(GuiStyle.button(s, Component.literal(">"), b -> flip(1), left() + 40, ny, 24, 14));
        add(GuiStyle.button(s, Component.literal("Replay"), b -> replay(), left() + 70, ny, 50, 14));
        add(GuiStyle.button(s, Component.translatable("gui.done"), b -> onClose(), left() + W - 62, ny, 50, 14));
    }

    private AbstractWidget add(AbstractWidget w) {
        return addRenderableWidget(w);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        GuiSkin s = skin();
        int x = left(), y = top();
        opening.render(g, width, height, x, y, W, H, () -> {
            s.panel(g, x, y, W, H);
            Component t = Component.literal(s.name() + "  " + (index + 1) + "/" + SKINS.length);
            g.drawString(font, t, x + (W - font.width(t)) / 2, y + 10, s.ink(), false);

            drawPrimitives(g, s, mx, my, x + 172, y + 26);
            super.render(g, mx, my, partial);
        });
    }

    /** The right-hand column: colours, insets, plaques, rows, slot, gauges, scrollbar, stamp. */
    private void drawPrimitives(GuiGraphics g, GuiSkin s, int mx, int my, int x, int y) {
        // the four text colours
        g.drawString(font, "ink", x, y, s.ink(), false);
        g.drawString(font, "dim", x + 30, y, s.dim(), false);
        g.drawString(font, "accent", x + 58, y, s.accent(), false);
        g.drawString(font, "warn", x + 104, y, s.warn(), false);

        // inset and plaque
        s.inset(g, x, y + 14, 70, 28);
        g.drawString(font, "inset", x + 6, y + 24, s.ink(), false);
        s.plaque(g, x + 76, y + 14, 70, 28);
        g.drawString(font, "plaque", x + 82, y + 24, s.ink(), false);

        // list rows: normal, picked, hovered
        s.inset(g, x, y + 48, 126, 40);
        for (int i = 0; i < 3; i++) {
            int ry = y + 50 + i * 12;
            boolean hover = mx >= x + 1 && mx < x + 125 && my >= ry && my < ry + 12;
            s.row(g, x + 1, ry, 124, 12, i == picked, hover);
            g.drawString(font, "Row " + (i + 1 + scroll), x + 5, ry + 2, i == picked ? s.accent() : s.ink(), false);
        }
        s.scrollbar(g, x + 130, y + 48, 40, 10, 3, scroll);

        // slot
        s.slot(g, x + 8, y + 96);
        g.renderItem(new ItemStack(Items.IRON_INGOT), x + 8, y + 96);
        g.drawString(font, "slot", x + 30, y + 100, s.dim(), false);

        // gauges
        float fill = (float) (slid / 10.0);
        Gauge.of(() -> fill).mark(() -> 0.7).render(g, s, x + 60, y + 98, 86, 8);
        Gauge.of(() -> fill).ticks(10).render(g, s, x + 60, y + 114, 86, 6);
        s.bar(g, x + 60, y + 130, 86, 6, fill, -1f);
        Gauge.of(() -> fill).vertical().ticks(5).lead(s::accent).render(g, s, x + 154, y + 14, 6, 74);

        // stamp
        long age = System.currentTimeMillis() - stampAt;
        if (age < 1700)
            GuiStyle.stamp(g, Component.literal("DONE"), x + 80, y + 150, age, s.warn() & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled())
            return true;
        int x = left() + 172, y = top() + 26;
        if (mx >= x + 1 && mx < x + 125 && my >= y + 50 && my < y + 86) {
            picked = (int) ((my - (y + 50)) / 12);
            skin().cue(GuiSkin.Cue.SEAT);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (!opening.settled())
            return true;
        scroll = Math.max(0, Math.min(7, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return !opening.settled() || super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (opening.closing())
            return true;
        if (super.keyPressed(key, scan, mods))
            return true; // a focused slider or stepper takes Left/Right first
        if (key == 263 || key == 262) {
            flip(key == 263 ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float partial) {
        // the opening dims the world
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
    public static final class Command_ {
        @SubscribeEvent
        public static void register(RegisterClientCommandsEvent e) {
            e.getDispatcher().register(Commands.literal("guiskins").executes(c -> {
                Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new SkinShowcaseScreen()));
                return Command.SINGLE_SUCCESS;
            }));
        }
    }
}
