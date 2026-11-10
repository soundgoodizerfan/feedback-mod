// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.machine.battery.BatteryStack;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.net.SuitPayload;
import io.github.soundgoodizerfan.feedback.net.SuitSyncPayload;
import io.github.soundgoodizerfan.feedback.suit.Ability;
import io.github.soundgoodizerfan.feedback.suit.ExoSuit;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The suit's diagnostic: a pan-and-zoom schematic of the body, four armour nodes down a spine with the battery bay hung off the chest,
 * power traces that carry moving dots while the suit draws (stopped and amber when it is cut out), and a column that gives the figures of
 * the node you click. Drag to pan, wheel to zoom. The column also fits and takes the battery and throws the five switches, the same ones the
 * wheel does. Figures read in words or numbers as the suit's gauges allow ({@link SuitReadout}); the schematic is a plain screen on the
 * suit skin with an {@link Opening}, like the skill tree.
 */
final class SuitScreen extends Screen {

    private static final GuiSkin SKIN = GuiSkin.SUIT;
    private static final int W = 440, H = 262, CANVAS_W = 252, COL_X = 266, COL_W = 164;

    /** A node of the schematic: its place in canvas units, size, the equipment slot it shows (null for the bay). */
    private record Node(String id, int x, int y, int w, int h, EquipmentSlot slot) {
    }

    private static final Node HEAD = new Node("head", -35, -88, 70, 30, EquipmentSlot.HEAD),
            CHEST = new Node("chest", -35, -42, 70, 30, EquipmentSlot.CHEST),
            LEGS = new Node("legs", -35, 4, 70, 30, EquipmentSlot.LEGS),
            FEET = new Node("feet", -35, 50, 70, 30, EquipmentSlot.FEET),
            BAY = new Node("bay", -118, -42, 52, 30, null);
    private static final Node[] NODES = { HEAD, CHEST, LEGS, FEET, BAY };
    /** The traces, pairs of nodes. */
    private static final Node[][] LINKS = { { BAY, CHEST }, { CHEST, HEAD }, { CHEST, LEGS }, { LEGS, FEET } };

    private final Opening opening = new Opening(SKIN);
    private float camX, camY, zoom = 1f;
    private boolean dragging, moved;
    private Node picked = CHEST;
    private final GuiStyle.Flash flash = new GuiStyle.Flash(1);

    SuitScreen() {
        super(Component.translatable("screen.feedback.suit"));
    }

    private int px() {
        return (width - W) / 2;
    }

    private int py() {
        return (height - H) / 2;
    }

    @Override
    protected void init() {
        opening.start();
        int x = px() + COL_X, y = py() + H - 12 - 5 * 16;
        Ability[] all = Ability.values();
        for (int i = 0; i < all.length; i++) {
            final int a = i;
            addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.suit.ability." + all[i].key()),
                    b -> PacketDistributor.sendToServer(new SuitPayload(SuitPayload.TOGGLE, a)), x + (i % 2) * (COL_W / 2 + 1), y + (i / 2) * 16, COL_W / 2 - 1, 14));
        }
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.suit.bay.take"),
                b -> PacketDistributor.sendToServer(new SuitPayload(SuitPayload.BAY_TAKE, 0)), x, y - 40, COL_W / 2 - 1, 14));
        addRenderableWidget(GuiStyle.button(SKIN, Component.translatable("gui.feedback.suit.bay.fit"), b -> {
            var inv = minecraft.player.getInventory();
            for (int s = 0; s < inv.getContainerSize(); s++)
                if (BatteryStack.is(inv.getItem(s)) && inv.getItem(s).getCount() == 1) {
                    PacketDistributor.sendToServer(new SuitPayload(SuitPayload.BAY_PUT, s));
                    return;
                }
        }, x + COL_W / 2 + 1, y - 40, COL_W / 2 - 1, 14));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void onClose() {
        opening.close();
    }

    @Override
    public void tick() {
        if (!SuitClient.worn())
            opening.close();
    }

    // --- drawing ------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (opening.shut()) {
            super.onClose();
            return;
        }
        opening.render(g, width, height, px(), py(), W, H, () -> body(g, mx, my, partial));
    }

    private void body(GuiGraphics g, int mx, int my, float partial) {
        int x = px(), y = py();
        SKIN.panel(g, x, y, W, H);
        g.drawString(font, title, x + 10, y + 9, SKIN.ink() | 0xFF000000, false);
        SuitSyncPayload s = SuitClient.state();
        long now = Util.getMillis();

        // the schematic
        int cx0 = x + 10, cy0 = y + 22, ch = H - 32;
        SKIN.inset(g, cx0, cy0, CANVAS_W, ch);
        g.enableScissor(cx0 + 1, cy0 + 1, cx0 + CANVAS_W - 1, cy0 + ch - 1);
        for (int gx = 0; gx < CANVAS_W; gx += 12) // a faint grid, so panning is felt
            g.fill(cx0 + gx, cy0 + 1, cx0 + gx + 1, cy0 + ch - 1, 0x10 << 24 | 0x35B6D0);
        for (int gy = 0; gy < ch; gy += 12)
            g.fill(cx0 + 1, cy0 + gy, cx0 + CANVAS_W - 1, cy0 + gy + 1, 0x10 << 24 | 0x35B6D0);
        g.pose().pushPose();
        g.pose().translate(cx0 + CANVAS_W / 2f + camX, cy0 + ch / 2f + camY, 0);
        g.pose().scale(zoom, zoom, 1);
        float flow = s.draw() > 0.05f ? now / 600f : 0f;
        int trace = s.cut() ? 0xFF << 24 | 0xFFA52E : 0xFF << 24 | 0x4FE3FF;
        for (Node[] l : LINKS) {
            int ax = l[0].x + l[0].w / 2, ay = l[0].y + l[0].h / 2, bx = l[1].x + l[1].w / 2, by = l[1].y + l[1].h / 2;
            GuiStyle.line(g, ax, ay, bx, by, 0x60 << 24 | 0x35B6D0);
            if (s.draw() > 0.05f)
                for (int d = 0; d < 3; d++) {
                    float f = (flow + d / 3f) % 1f, dx = Mth.lerp(f, ax, bx), dy = Mth.lerp(f, ay, by);
                    g.fill((int) dx - 1, (int) dy - 1, (int) dx + 1, (int) dy + 1, trace);
                }
        }
        for (Node n : NODES)
            node(g, n, s, now);
        g.pose().popPose();
        g.disableScissor();

        column(g, s);
        super.render(g, mx, my, partial);
    }

    private ItemStack stackOf(Node n) {
        if (minecraft.player == null)
            return ItemStack.EMPTY;
        return n.slot == null ? ExoSuit.battery(minecraft.player.getItemBySlot(EquipmentSlot.CHEST)) : minecraft.player.getItemBySlot(n.slot);
    }

    private void node(GuiGraphics g, Node n, SuitSyncPayload s, long now) {
        ItemStack stack = stackOf(n);
        boolean sel = n == picked;
        boolean has = !stack.isEmpty();
        g.fill(n.x, n.y, n.x + n.w, n.y + n.h, 0xF0 << 24 | 0x0A0F12);
        int rim = sel ? SKIN.accent() | 0xFF000000 : has ? 0xC0 << 24 | 0x35B6D0 : 0x60 << 24 | 0x7C99A6;
        g.renderOutline(n.x, n.y, n.w, n.h, rim);
        if (sel && (now / 400) % 2 == 0)
            g.renderOutline(n.x - 2, n.y - 2, n.w + 4, n.h + 4, 0x50 << 24 | 0x4FE3FF);
        g.renderItem(stack, n.x + 4, n.y + (n.h - 16) / 2);
        Component label = Component.translatable("screen.feedback.suit.node." + n.id);
        g.drawString(font, label, n.x + 24, n.y + 5, has ? SKIN.ink() | 0xFF000000 : SKIN.dim() | 0xFF000000, false);
        // the node's own line: what it is doing now
        Component line = switch (n.id) {
            case "bay" -> SuitReadout.charge(s);
            case "head" -> Component.translatable(abilityLine(Ability.NIGHT, Ability.THERMAL));
            case "chest" -> Component.translatable(abilityLine(Ability.STRENGTH, Ability.PLATING));
            default -> Component.translatable(abilityLine(Ability.JUMP_DASH, Ability.JUMP_DASH));
        };
        g.drawString(font, line, n.x + 24, n.y + 17, SKIN.dim() | 0xFF000000, false);
    }

    private static String abilityLine(Ability a, Ability b) {
        boolean live = SuitClient.live(a) || SuitClient.live(b), can = SuitClient.can(a) || SuitClient.can(b);
        return !can ? "gui.feedback.suit.state.none" : live ? "gui.feedback.suit.state.on" : SuitClient.on(a) || SuitClient.on(b) ? "gui.feedback.suit.state.starved" : "gui.feedback.suit.state.off";
    }

    /** The right-hand column: the picked node's figures, in words or numbers as the gauges allow. */
    private void column(GuiGraphics g, SuitSyncPayload s) {
        int x = px() + COL_X, y = py() + 22;
        SKIN.inset(g, x, y, COL_W, H - 32 - 5 * 16 - 46);
        int tx = x + 6, ty = y + 6;
        ItemStack stack = stackOf(picked);
        g.drawString(font, Component.translatable("screen.feedback.suit.node." + picked.id), tx, ty, SKIN.accent() | 0xFF000000, false);
        ty += 12;
        if (stack.isEmpty()) {
            g.drawString(font, Component.translatable("screen.feedback.suit.empty"), tx, ty, SKIN.dim() | 0xFF000000, false);
            return;
        }
        ModularFigures f = ModularStacks.figures(stack);
        if (picked == BAY) {
            var t = BatteryStack.state(stack);
            line(g, tx, ty, "screen.feedback.suit.fig.charge", SuitReadout.charge(s));
            if (t != null)
                line(g, tx, ty + 11, "screen.feedback.suit.fig.cells", Component.literal(Integer.toString(t.getInt("Cells"))));
            line(g, tx, ty + 22, "screen.feedback.suit.fig.draw", figure(s.draw(), "A", 4f, 14f));
            return;
        }
        if (f == null)
            return;
        float dur = stack.isDamageableItem() ? 1f - stack.getDamageValue() / (float) stack.getMaxDamage() : 1f;
        line(g, tx, ty, "screen.feedback.suit.fig.condition", Component.literal(Math.round(shown(dur, s) * 100) + "%"));
        line(g, tx, ty + 11, "screen.feedback.suit.fig.mass", figure(f.get(Fig.MASS), "kg", 8f, 20f));
        float drive = f.get(Fig.SUIT_DRIVE);
        line(g, tx, ty + 22, "screen.feedback.suit.fig.drive", drive > 0f ? figure(drive, "A", 11f, 33f) : Component.translatable("screen.feedback.suit.fig.none"));
        if (drive > 0f)
            line(g, tx, ty + 33, "screen.feedback.suit.fig.loss", Component.literal(Math.round(shown(f.get(Fig.SUIT_LOSS), s) * 100) + "%"));
        if (f.get(Fig.SUIT_GAUGE) > 0f)
            line(g, tx, ty + 44, "screen.feedback.suit.fig.gauge", Component.literal(Math.round(f.get(Fig.SUIT_GAUGE) * 100) + "%"));
        if (f.get(Fig.SUIT_SENSE) > 0f)
            line(g, tx, ty + 55, "screen.feedback.suit.fig.sense", Component.literal(Math.round(f.get(Fig.SUIT_SENSE)) + ""));
        if (f.get(Fig.SUIT_BAY) > 0f)
            line(g, tx, ty + 66, "screen.feedback.suit.fig.bay", Component.translatable("screen.feedback.suit.fig.yes"));
        String up = io.github.soundgoodizerfan.feedback.suit.SuitModules.key(Math.round(f.get(Fig.SUIT_MODULE)));
        if (!up.isEmpty())
            line(g, tx, ty + 77, "screen.feedback.suit.fig.upgrade", Component.translatable("modular.feedback.variant." + up));
        g.drawString(font, SuitReadout.mass(s), tx, ty + 91, SKIN.dim() | 0xFF000000, false);
        g.drawString(font, SuitReadout.heat(s), tx, ty + 102, flash.colour(0, SuitReadout.heat(s).getString(), SKIN.dim() | 0xFF000000, SKIN.accent() | 0xFF000000), false);
    }

    private static float shown(float v, SuitSyncPayload s) {
        return SuitReadout.shown(v, s);
    }

    /** A figure as a number when a gauge is fitted, else as a word against two bands. */
    private static Component figure(float v, String unit, float mid, float high) {
        SuitSyncPayload s = SuitClient.state();
        if (SuitReadout.figures(s))
            return Component.literal(Math.round(shown(v, s) * 10f) / 10f + " " + unit);
        return Component.translatable("screen.feedback.suit.band." + (v < mid ? "low" : v < high ? "mid" : "high"));
    }

    private void line(GuiGraphics g, int x, int y, String key, Component value) {
        g.drawString(font, Component.translatable(key), x, y, SKIN.dim() | 0xFF000000, false);
        g.drawString(font, value, x + COL_W - 12 - font.width(value), y, SKIN.ink() | 0xFF000000, false);
    }

    // --- input ------------------------------------------------------------------------------------

    private boolean inCanvas(double mx, double my) {
        return mx >= px() + 10 && mx < px() + 10 + CANVAS_W && my >= py() + 22 && my < py() + H - 10;
    }

    private Node nodeAt(double mx, double my) {
        double wx = (mx - (px() + 10 + CANVAS_W / 2.0 + camX)) / zoom, wy = (my - (py() + 22 + (H - 32) / 2.0 + camY)) / zoom;
        for (Node n : NODES)
            if (wx >= n.x && wx < n.x + n.w && wy >= n.y && wy < n.y + n.h)
                return n;
        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!opening.settled())
            return true;
        if (super.mouseClicked(mx, my, button))
            return true;
        dragging = button == 0 && inCanvas(mx, my);
        moved = false;
        return dragging;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!dragging)
            return super.mouseDragged(mx, my, button, dx, dy);
        if (moved || Math.abs(dx) + Math.abs(dy) > 1) {
            moved = true;
            camX = Mth.clamp(camX + (float) dx, -160, 160);
            camY = Mth.clamp(camY + (float) dy, -140, 140);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging && !moved) {
            Node n = nodeAt(mx, my);
            if (n != null) {
                picked = n;
                GuiSkin.play(SKIN.click(), 1.3f, 0.5f);
            }
        }
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (!opening.settled() || !inCanvas(mx, my))
            return super.mouseScrolled(mx, my, sx, sy);
        zoom = Mth.clamp(zoom * (sy > 0 ? 1.15f : 1f / 1.15f), 0.6f, 2.2f);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (SuitKeys.SCREEN.matches(key, scan) && !opening.closing()) {
            opening.close();
            return true;
        }
        return opening.closing() || super.keyPressed(key, scan, mods);
    }
}
