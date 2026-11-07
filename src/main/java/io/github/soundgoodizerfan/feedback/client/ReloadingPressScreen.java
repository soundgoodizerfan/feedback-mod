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
import io.github.soundgoodizerfan.feedback.net.PressPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.BulletShape;
import io.github.soundgoodizerfan.feedback.weapon.Cartridge;
import io.github.soundgoodizerfan.feedback.weapon.GunCalibres;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * The Reloading Press ({@code docs/specs/feedback_guns_spec.md} section 5): case, charge, bullet and payload in,
 * a round out. Three panes on the {@link GuiSkin#FORGE} panel: the components (case, one or two charges, a bullet
 * with an optional core inside its jacket, a payload), the die (the bore, stepped, and the shape it stamps into the
 * blank), and the round's own spec sheet, read as it would be seated now -- the press's only help, because the press
 * does not know when a charge is enough. The seated pressure is a bar against the case's rating, red past it.
 */
public class ReloadingPressScreen extends SkinnedScreen<ReloadingPressMenu> {

    /** The panes: components left, die right, the sheet under both. Panel-relative. */
    private static final int PARTS_X = 8, PARTS_Y = 20, PARTS_W = 124, PARTS_H = 96;
    private static final int DIE_X = 138, DIE_Y = 20, DIE_W = 78, DIE_H = 96;
    private static final int SHEET_X = 8, SHEET_Y = 122, SHEET_W = 208, SHEET_H = 54;
    private static final int BAR_Y = 166;

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
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + DIE_X + 5, topPos + 34, DIE_W - 10, 14, "<", ">",
                () -> Component.translatable("modular.feedback.variant." + GunCalibres.nearest(bore).name()).getString() + ", " + bore + " mm",
                dir -> setBore(bore + dir), true, FSounds.UI_DETENT::get));
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + DIE_X + 5, topPos + 62, DIE_W - 10, 14, "<", ">",
                () -> shapeName(die), this::stepDie, true, FSounds.UI_DETENT::get));
        addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.reloading_press.seat"), b -> {
                    if (gauge < 0)
                        skin.cue(GuiSkin.Cue.REJECT);
                    send(true);
                },
                leftPos + DIE_X + 5, topPos + 88, 46, 16, () -> SoundEvents.PISTON_EXTEND));
    }

    private static String shapeName(int index) {
        return Component.translatable(index < 0 ? "gui.feedback.reloading_press.shape.as_is"
                : "gui.feedback.reloading_press.shape." + BulletShape.values()[index].getSerializedName()).getString();
    }

    /** Steps the die through the shapes and "as is": a wrap, so every shape is two clicks from either side. */
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

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.inset(g, leftPos + PARTS_X, topPos + PARTS_Y, PARTS_W, PARTS_H);
        skin.inset(g, leftPos + DIE_X, topPos + DIE_Y, DIE_W, DIE_H);
        skin.inset(g, leftPos + SHEET_X, topPos + SHEET_Y, SHEET_W, SHEET_H);
        for (int[] xy : ReloadingPressMenu.SLOT_XY)
            skin.slot(g, leftPos + xy[0], topPos + xy[1]);
        for (int c = 0; c < 9; c++)
            skin.slot(g, leftPos + ReloadingPressMenu.HOTBAR_X + c * 18, topPos + ReloadingPressMenu.HOTBAR_Y);
        // The line the round is fed along, from the lever to the slot it drops into.
        int ly = topPos + ReloadingPressMenu.SLOT_XY[6][1] + 8;
        g.fill(leftPos + DIE_X + 53, ly, leftPos + ReloadingPressMenu.SLOT_XY[6][0] - 2, ly + 1, skin.dim());
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        super.renderLabels(g, mouseX, mouseY);
        String[] names = {"case", "charge", null, "projectile", null, "payload"};
        for (int i = 0; i < names.length; i++)
            if (names[i] != null)
                g.drawString(font, Component.translatable("gui.feedback.reloading_press." + names[i]),
                        14, ReloadingPressMenu.SLOT_XY[i][1] + 4, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.mix"),
                108, ReloadingPressMenu.SLOT_XY[2][1] + 4, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.core"),
                108, ReloadingPressMenu.SLOT_XY[4][1] + 4, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.bore"), DIE_X + 6, 25, skin.dim(), false);
        g.drawString(font, Component.translatable("gui.feedback.reloading_press.die"), DIE_X + 6, 52, skin.dim(), false);

        long now = net.minecraft.Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100L) / 1000f;
        lastFrame = now;
        var materials = ClientMaterials.on(minecraft.level);
        var cas = ReloadingPressBlockEntity.located(materials, menu.getSlot(ReloadingPressBlockEntity.CASE).getItem());
        var blank = ReloadingPressBlockEntity.located(materials, menu.getSlot(ReloadingPressBlockEntity.PROJECTILE).getItem());
        var coreFound = ReloadingPressBlockEntity.located(materials, menu.getSlot(ReloadingPressBlockEntity.CORE).getItem());
        boolean shot = blank.isPresent() && blank.get().form().equals("nugget");
        BulletShape shape = shapeSeated(blank, shot);
        var projectile = blank.map(b -> b.id()).or(() -> cas.map(c -> c.id()));
        var round0 = projectile.map(p -> new Cartridge(bore, p, java.util.List.of(), p, Optional.empty(), shot, shape,
                shot ? Optional.empty() : coreFound.map(c -> c.id()), Optional.empty()));

        // What the shape and the jacket do to the shot, as multipliers on a round nose of one metal.
        int sy = SHEET_Y + 6;
        String shapeLine = Component.translatable("gui.feedback.reloading_press.row.shape").getString() + " "
                + shapeName(shape.ordinal()) + "   "
                + String.format("%s %.2f  %s %.2f  %s %.2f",
                Component.translatable("gui.feedback.reloading_press.row.drag").getString(), round0.map(r -> r.drag(materials)).orElse(shape.drag()),
                Component.translatable("gui.feedback.reloading_press.row.pierce").getString(), round0.map(r -> shape.pierce() * (r.sabot(materials) ? FTuning.GUN_SABOT_PIERCE : 1f)).orElse(shape.pierce()),
                Component.translatable("gui.feedback.reloading_press.row.hurt").getString(), shape.damage());
        g.drawString(font, font.plainSubstrByWidth(shapeLine, SHEET_W - 12), SHEET_X + 6, sy + 24, skin.dim(), false);

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
                shot ? Optional.empty() : coreFound.map(c -> c.id()), Optional.empty());
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
    private BulletShape shapeSeated(Optional<io.github.soundgoodizerfan.feedback.process.Material.Located> blank, boolean shot) {
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
        int x = right ? SHEET_X + SHEET_W / 2 + 4 : SHEET_X + 6, end = right ? SHEET_X + SHEET_W - 6 : SHEET_X + SHEET_W / 2 - 4;
        g.drawString(font, label, x, y, skin.dim(), false);
        g.drawString(font, figure, end - font.width(figure), y, colour, false);
    }
}
