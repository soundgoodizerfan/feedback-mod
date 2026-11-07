/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.net.PressPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.BulletShape;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunCalibres;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * The Reloading Press ({@code docs/specs/feedback_guns_spec.md} section 5), laid out as a bench: a rail of component
 * sockets along the top (case, one or two charges, a bullet with an optional core, a payload), under it the round
 * being built, drawn live in the colours of what is seated and in the bullet's real shape, with a lead from each socket
 * to its part. A strip at the foot of the view holds the die (the bore and the shape it stamps) and the lever. The sheet
 * under the view is the round's own spec read as it would be seated now, the only help the press gives because it does
 * not know when a charge is enough. Iron and brass ({@link GuiSkin#FORGE}). The round drawing is screen-local colour,
 * like the Skillet's pan: it is the subject of the screen.
 */
public class ReloadingPressScreen extends SkinnedScreen<ReloadingPressMenu> {

    /** Panel-relative panes: the view with the rail and the round, the sheet under it. */
    private static final int VIEW_X = 8, VIEW_Y = 18, VIEW_W = 304, VIEW_H = 134;
    private static final int SHEET_X = 8, SHEET_Y = 158, SHEET_W = 304, SHEET_H = 48;
    private static final int BAR_Y = 198;
    /** The round's drawing: where the case sits, the bullet's start, and the centre line. */
    private static final int CASE_X = 52, CASE_W = 110, CASE_H = 28, BULLET_X = 150, BULLET_L = 54, BULLET_H = 13, CENTRE_Y = 96;

    private int bore;
    /** Which shape the die stamps: an index into {@link BulletShape#values}, or -1 to seat the blank as it is. */
    private int die;
    /** The pressure bar as drawn, easing toward the figure; negative until there is one. */
    private float gauge = -1;
    private long lastFrame;

    public ReloadingPressScreen(ReloadingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.FORGE);
        this.bore = menu.openedBore();
        this.die = menu.openedDie();
        imageWidth = ReloadingPressMenu.WIDTH;
        imageHeight = ReloadingPressMenu.HEIGHT;
        titleLabelX = 12;
        inventoryLabelY = 10_000; // only the hotbar is shown
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + 16, topPos + 130, 118, 14, "<", ">",
                () -> Component.translatable("modular.feedback.variant." + GunCalibres.nearest(bore).name()).getString() + ", " + bore + " mm",
                dir -> setBore(bore + dir), true, FSounds.UI_DETENT::get));
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + 150, topPos + 130, 118, 14, "<", ">",
                () -> shapeName(die), this::stepDie, true, FSounds.UI_DETENT::get));
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.reloading_press.seat"), b -> {
                    if (gauge < 0)
                        skin.cue(GuiSkin.Cue.REJECT);
                    send(true);
                },
                leftPos + 276, topPos + 128, 30, 18, () -> SoundEvents.PISTON_EXTEND));
    }

    private static String shapeName(int index) {
        return Component.translatable(index < 0 ? "gui.feedback.reloading_press.shape.as_is"
                : "gui.feedback.reloading_press.shape." + BulletShape.values()[index].getSerializedName()).getString();
    }

    /** Steps the die through the shapes and "as is": a wrap, so every shape is a few clicks from either side. */
    private void stepDie(int dir) {
        int n = BulletShape.values().length + 1;
        die = Math.floorMod(die + 1 + dir, n) - 1;
        send(false);
    }

    private void setBore(int value) {
        bore = Mth.clamp(value, io.github.soundgoodizerfan.feedback.weapon.GunSpec.MIN_BORE_MM, io.github.soundgoodizerfan.feedback.weapon.GunSpec.MAX_BORE_MM);
        send(false);
    }

    private void send(boolean seat) {
        PacketDistributor.sendToServer(new PressPayload(menu.getPos(), bore, die, seat));
    }

    /** The material of what is in {@code slot}, if it is one. */
    private Optional<Material.Located> seated(int slot) {
        return ReloadingPressBlockEntity.located(ClientMaterials.on(minecraft.level), menu.getSlot(slot).getItem());
    }

    private static int tint(Optional<Material.Located> found) {
        return found.map(l -> 0xFF000000 | MaterialColors.of(l.id())).orElse(0);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.inset(g, leftPos + VIEW_X, topPos + VIEW_Y, VIEW_W, VIEW_H);
        skin.inset(g, leftPos + SHEET_X, topPos + SHEET_Y, SHEET_W, SHEET_H);
        for (int i = 0; i < ReloadingPressMenu.SLOT_XY.length; i++) {
            int[] xy = ReloadingPressMenu.SLOT_XY[i];
            skin.plaque(g, leftPos + xy[0] - 5, topPos + xy[1] - 5, i == ReloadingPressBlockEntity.OUTPUT ? 26 : 26, 26);
            skin.slot(g, leftPos + xy[0], topPos + xy[1]);
        }
        for (int c = 0; c < 9; c++)
            skin.slot(g, leftPos + ReloadingPressMenu.HOTBAR_X + c * 18, topPos + ReloadingPressMenu.HOTBAR_Y);
        drawRound(g);
    }

    /** The round as it would be seated, drawn from what is in the sockets, with a lead from each socket to its part. */
    private void drawRound(GuiGraphics g) {
        int ox = leftPos, oy = topPos;
        var materials = ClientMaterials.on(minecraft.level);
        var cas = seated(ReloadingPressBlockEntity.CASE);
        var blank = seated(ReloadingPressBlockEntity.PROJECTILE);
        var core = seated(ReloadingPressBlockEntity.CORE);
        var payload = seated(ReloadingPressBlockEntity.PAYLOAD);
        boolean shot = blank.isPresent() && blank.get().form().equals("nugget");
        BulletShape shape = shapeSeated(blank, shot);
        int faint = GuiSkin.lerp(skin.dim(), 0x00000000, 0.55f), edge = skin.dim();

        // The case: a rim and a body, filled in the case metal, or only its outline while the socket is empty.
        int caseColour = tint(cas);
        int cy0 = oy + CENTRE_Y - CASE_H / 2;
        box(g, ox + CASE_X - 6, oy + CENTRE_Y - CASE_H / 2 - 2, 6, CASE_H + 4, caseColour, edge);
        box(g, ox + CASE_X, cy0, CASE_W, CASE_H, caseColour, edge);

        // The charge: powder along the case, a share of it by count, in the blend's own colours.
        int total = 0;
        var parts = new java.util.ArrayList<int[]>();
        for (int slot : new int[]{ReloadingPressBlockEntity.CHARGE, ReloadingPressBlockEntity.CHARGE_B}) {
            var stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty())
                continue;
            var found = seated(slot);
            int n = Math.min(stack.getCount(), FTuning.GUN_CHARGE_MAX);
            int colour = found.isPresent() ? 0xFF000000 | MaterialColors.of(found.get().id()) : 0xFF8A8A8A;
            parts.add(new int[]{n, colour});
            total += n;
        }
        int px = ox + CASE_X + 3;
        int span = CASE_W - 6;
        for (int[] part : parts) {
            int w = Math.max(2, Math.round(span * Math.min(1f, total / (float) FTuning.GUN_CHARGE_MAX) * part[0] / total));
            g.fill(px, cy0 + 4, px + w, cy0 + CASE_H - 4, part[1]);
            px += w;
        }

        // The bullet, in its shape. A core shows inside the jacket; a payload is a bead at the tip.
        int bx = ox + BULLET_X, by = oy + CENTRE_Y;
        int jacket = tint(blank);
        if (shot)
            drawShot(g, bx + 10, by, shape, jacket != 0 ? jacket : faint, edge);
        else {
            drawBullet(g, bx, by, shape, 1f, 1f, jacket != 0 ? jacket : 0, edge);
            if (core.isPresent())
                drawBullet(g, bx + 6, by, shape, 0.5f, 0.8f, tint(core), 0);
        }
        if (payload.isPresent()) {
            int cx = bx + BULLET_L - (shot ? 2 : 10), c = tint(payload);
            g.fill(cx - 2, by - 2, cx + 3, by + 3, 0xFF101010);
            g.fill(cx - 1, by - 1, cx + 2, by + 2, c);
        }

        // Leads: from each socket down to the part it feeds, bright once that socket is filled.
        lead(g, ReloadingPressBlockEntity.CASE, ox + CASE_X - 6, CENTRE_Y);
        lead(g, ReloadingPressBlockEntity.CHARGE, -1, CENTRE_Y - CASE_H / 2 + 4);
        lead(g, ReloadingPressBlockEntity.CHARGE_B, -1, CENTRE_Y - CASE_H / 2 + 4);
        lead(g, ReloadingPressBlockEntity.PROJECTILE, -1, CENTRE_Y - BULLET_H + 1 + (shot ? 10 : 0));
        lead(g, ReloadingPressBlockEntity.CORE, -1, CENTRE_Y - 5);
        lead(g, ReloadingPressBlockEntity.PAYLOAD, bx + BULLET_L - (shot ? 2 : 10), CENTRE_Y - 4);
        // The line the round is fed along, from the case to the socket it drops into.
        int[] out = ReloadingPressMenu.SLOT_XY[ReloadingPressBlockEntity.OUTPUT];
        g.fill(bx + BULLET_L + 6, by, ox + out[0] - 7, by + 1, faint);
        g.fill(ox + out[0] - 7, Math.min(by, oy + out[1] + 8), ox + out[0] - 6, Math.max(by, oy + out[1] + 8) + 1, faint);
    }

    /** A box with a body and a rim, or a rim alone when {@code fill} is 0. */
    private void box(GuiGraphics g, int x, int y, int w, int h, int fill, int rim) {
        if (fill != 0)
            g.fill(x, y, x + w, y + h, fill);
        g.fill(x, y, x + w, y + 1, rim);
        g.fill(x, y + h - 1, x + w, y + h, rim);
        g.fill(x, y, x + 1, y + h, rim);
        g.fill(x + w - 1, y, x + w, y + h, rim);
    }

    /** One lead: down from a socket's foot to the part's top, then across to {@code toX} (or straight, with -1). */
    private void lead(GuiGraphics g, int slot, int toX, int toY) {
        int[] xy = ReloadingPressMenu.SLOT_XY[slot];
        boolean filled = !menu.getSlot(slot).getItem().isEmpty();
        int colour = filled ? skin.ink() : GuiSkin.lerp(skin.dim(), 0x00000000, 0.6f);
        int x0 = leftPos + xy[0] + 8, y0 = topPos + xy[1] + 22;
        if (toX < 0) {
            g.fill(x0, y0, x0 + 1, topPos + toY, colour);
            return;
        }
        int bend = topPos + toY - 12;
        if (slot == ReloadingPressBlockEntity.CASE) {
            g.fill(x0, y0, x0 + 1, topPos + toY, colour);
            g.fill(x0, topPos + toY, toX, topPos + toY + 1, colour);
            return;
        }
        g.fill(x0, y0, x0 + 1, bend, colour);
        g.fill(Math.min(x0, toX), bend, Math.max(x0, toX) + 1, bend + 1, colour);
        g.fill(toX, bend, toX + 1, topPos + toY, colour);
    }

    /**
     * A bullet from its base at ({@code x}, centre {@code y}) to its tip, one column at a time, in the shape's own
     * profile. {@code scaleH} and {@code scaleL} shrink it for the core drawn inside the jacket. Placeholder drawing:
     * the profiles are only for the eye, the figures live in {@link BulletShape}.
     */
    private void drawBullet(GuiGraphics g, int x, int y, BulletShape shape, float scaleH, float scaleL, int fill, int rim) {
        int length = Math.round(BULLET_L * scaleL), height = Math.round(BULLET_H * scaleH);
        if (shape == BulletShape.ROUND_BALL)
            length = Math.min(length, 2 * height);
        for (int i = 0; i < length; i++) {
            float t = length <= 1 ? 0 : i / (float) (length - 1);
            double half = switch (shape) {
                case ROUND_BALL -> Math.sqrt(Math.max(0, height * height - (i - height) * (double) (i - height)));
                case FLAT_NOSE -> height * (1 - 0.25 * t * t);
                case POINTED -> height * Math.pow(1 - t, 0.75);
                default -> height * Math.sqrt(1 - 0.85 * t * t);
            };
            int h = (int) Math.round(half);
            if (h < 1 && i == length - 1)
                continue;
            if (fill != 0)
                g.fill(x + i, y - h, x + i + 1, y + h + 1, fill);
            if (rim != 0) {
                g.fill(x + i, y - h, x + i + 1, y - h + 1, rim);
                g.fill(x + i, y + h, x + i + 1, y + h + 1, rim);
            }
        }
        if (shape == BulletShape.HOLLOW_POINT && fill != 0 && scaleH == 1f)
            g.fill(x + length - 8, y - 3, x + length, y + 4, 0xFF101010);
    }

    /** Shot as the pellets a shell throws: how many and how big follow the shape. */
    private void drawShot(GuiGraphics g, int x, int y, BulletShape shape, int fill, int rim) {
        int[][] at = switch (shape) {
            case BIRDSHOT -> new int[][]{{0, -9}, {8, -6}, {16, -9}, {24, -5}, {32, -8}, {4, 2}, {12, 5}, {20, 1}, {28, 6}, {36, 2}, {0, 9}, {16, 10}};
            case LARGE_SHOT -> new int[][]{{4, -6}, {22, 4}, {38, -4}};
            case FLECHETTE -> new int[][]{{0, -9}, {0, -3}, {0, 3}, {0, 9}};
            default -> new int[][]{{2, -8}, {18, -4}, {34, -9}, {10, 6}, {28, 8}};
        };
        int size = switch (shape) {
            case BIRDSHOT -> 2;
            case LARGE_SHOT -> 8;
            case FLECHETTE -> 3;
            default -> 5;
        };
        for (int[] p : at) {
            if (shape == BulletShape.FLECHETTE) {
                g.fill(x + p[0], y + p[1], x + p[0] + 30, y + p[1] + 1, fill);
                g.fill(x + p[0] + 30, y + p[1] - 1, x + p[0] + 33, y + p[1] + 2, rim);
            } else {
                g.fill(x + p[0], y + p[1], x + p[0] + size, y + p[1] + size, fill);
                g.fill(x + p[0], y + p[1], x + p[0] + size, y + p[1] + 1, rim);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String[] names = {"case", "charge", "mix", "projectile", "core", "payload"};
        for (int i = 0; i < names.length; i++) {
            Component label = Component.translatable("gui.feedback.reloading_press." + names[i]);
            int cx = ReloadingPressMenu.SLOT_XY[i][0] + 8;
            g.drawString(font, label, cx - font.width(label) / 2, VIEW_Y + 6, skin.dim(), false);
        }
        Component roundLabel = Component.translatable("gui.feedback.reloading_press.round");
        g.drawString(font, roundLabel, ReloadingPressMenu.SLOT_XY[6][0] + 8 - font.width(roundLabel) / 2, ReloadingPressMenu.SLOT_XY[6][1] - 14, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.bore"), 18, 120, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.die"), 152, 120, skin.dim(), false);

        long now = net.minecraft.Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100L) / 1000f;
        lastFrame = now;
        var materials = ClientMaterials.on(minecraft.level);
        var cas = seated(ReloadingPressBlockEntity.CASE);
        var blank = seated(ReloadingPressBlockEntity.PROJECTILE);
        var coreFound = seated(ReloadingPressBlockEntity.CORE);
        boolean shot = blank.isPresent() && blank.get().form().equals("nugget");
        BulletShape shape = shapeSeated(blank, shot);
        Optional<ResourceLocation> projectile = blank.map(Material.Located::id).or(() -> cas.map(Material.Located::id));
        Optional<Cartridge> shapeRound = projectile.map(p -> new Cartridge(bore, p, java.util.List.of(), p, Optional.empty(), shot, shape,
                shot ? Optional.empty() : coreFound.map(Material.Located::id), Optional.empty()));

        // What the shape and the jacket do to the shot, as multipliers on a round nose of one metal.
        int sy = SHEET_Y + 6;
        String shapeLine = Component.translatable("gui.feedback.reloading_press.row.shape").getString() + " "
                + shapeName(shape.ordinal()) + "    "
                + String.format("%s %.2f   %s %.2f   %s %.2f",
                Component.translatable("gui.feedback.reloading_press.row.drag").getString(), shapeRound.map(r -> r.drag(materials)).orElse(shape.drag()),
                Component.translatable("gui.feedback.reloading_press.row.pierce").getString(),
                shapeRound.map(r -> shape.pierce() * (r.sabot(materials) ? FTuning.GUN_SABOT_PIERCE : 1f)).orElse(shape.pierce()),
                Component.translatable("gui.feedback.reloading_press.row.hurt").getString(), shape.damage());
        g.drawString(font, font.plainSubstrByWidth(shapeLine, SHEET_W - 12), SHEET_X + 6, sy + 22, skin.dim(), false);

        var charges = new java.util.ArrayList<Cartridge.Charge>();
        for (int slot : new int[]{ReloadingPressBlockEntity.CHARGE, ReloadingPressBlockEntity.CHARGE_B}) {
            var stack = menu.getSlot(slot).getItem();
            if (stack.has(io.github.soundgoodizerfan.feedback.registry.FWeapons.MIXTURE.get())) {
                charges.addAll(ReloadingPressBlockEntity.partsOf(stack, materials, minecraft.level));
                continue;
            }
            var found = ReloadingPressBlockEntity.located(materials, stack);
            if (found.isPresent() && found.get().material().propellant().isPresent())
                charges.add(new Cartridge.Charge(found.get().id(), Math.min(stack.getCount(), FTuning.GUN_CHARGE_MAX)));
        }
        if (cas.isEmpty() || charges.isEmpty()) {
            g.drawString(font, Component.translatable("gui.feedback.reloading_press.idle"), SHEET_X + 6, sy, skin.dim(), false);
            gauge = -1;
            return;
        }
        var round = new Cartridge(bore, cas.get().id(), charges, projectile.get(), Optional.empty(), shot, shape,
                shot ? Optional.empty() : coreFound.map(Material.Located::id), Optional.empty());
        var load = round.load(materials);
        var rating = round.caseRating(materials);
        if (load.isEmpty() || rating.isEmpty()) {
            g.drawString(font, Component.translatable("message.feedback.press.no_blend"), SHEET_X + 6, sy, skin.warn(), false);
            gauge = -1;
            return;
        }
        float peak = load.get().peakPressure(), limit = rating.get().value();
        int tone = !load.get().coil() && peak > limit ? skin.warn() : skin.ink();
        row(g, sy, Component.translatable(load.get().coil() ? "gui.feedback.reloading_press.row.drive" : "gui.feedback.reloading_press.row.charge"),
                String.valueOf(round.charge()), tone);
        row(g, sy, Component.translatable("gui.feedback.reloading_press.row.peak"), String.format("%.1f Pu", peak), tone, true);
        row(g, sy + 12, Component.translatable("gui.feedback.reloading_press.row.case"), String.format("%.1f Pu", limit), skin.ink());
        row(g, sy + 12, Component.translatable("gui.feedback.reloading_press.row.pellets"),
                String.valueOf(round.pellets()), skin.ink(), true);
        // The bar: full scale is half again the rating, the rating the tick, past it red.
        float target = limit > 0 ? peak / limit : 1.5f;
        gauge = gauge < 0 ? target : gauge + (target - gauge) * Math.min(1, dt * 10);
        skin.bar(g, SHEET_X + 6, BAR_Y, SHEET_W - 12, 4, gauge / 1.5f, 2 / 3f);
    }

    /** The shape the press would seat for what is in the bullet slot now, as the server will read it. */
    private BulletShape shapeSeated(Optional<Material.Located> blank, boolean shot) {
        BulletShape chosen = die < 0 ? null : BulletShape.values()[die];
        if (shot)
            return chosen != null && chosen.shot() ? chosen : BulletShape.BUCKSHOT;
        BulletShape held = blank.flatMap(b -> BulletShape.ofForm(b.form())).orElse(BulletShape.ROUND_NOSE);
        return chosen == null || chosen.shot() ? held : chosen;
    }

    private void row(GuiGraphics g, int y, Component label, String figure, int colour) {
        row(g, y, label, figure, colour, false);
    }

    /** A label and its figure, left half or (right) right half of the sheet. */
    private void row(GuiGraphics g, int y, Component label, String figure, int colour, boolean right) {
        int x = right ? SHEET_X + SHEET_W / 2 + 6 : SHEET_X + 6, end = right ? SHEET_X + SHEET_W - 6 : SHEET_X + SHEET_W / 2 - 6;
        g.drawString(font, label, x, y, skin.dim(), false);
        g.drawString(font, figure, end - font.width(figure), y, colour, false);
    }
}
