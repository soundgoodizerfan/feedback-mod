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

import io.github.soundgoodizerfan.feedback.net.GunStandPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Barrel;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Launcher;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Part;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Sight;
import io.github.soundgoodizerfan.feedback.weapon.GunDesign.Wall;
import io.github.soundgoodizerfan.feedback.weapon.GunLayout;
import io.github.soundgoodizerfan.feedback.weapon.GunStandMenu;
import io.github.soundgoodizerfan.feedback.weapon.GunTooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import io.github.soundgoodizerfan.feedback.weapon.GunStandBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The Gun Stand's editor ({@code feedback_guns_spec.md} section 4), drawn as a blueprint: the gun
 * on a grid, its parts labelled, and the drawing reshaping as the design does -- a long barrel
 * draws long, a thick wall thick, a coil wound. Click a part and its settings show in the
 * specification; the sample slot sets the selected part's metal (spec 2: a gun is parts, each
 * with a material). The figures the design makes read live off the material system, the chamber
 * in red when the metal is asked for too much, so a bad gun is seen on the Stand and not found by
 * firing it. Every edit goes to the stand; Print, in the title block, stamps it onto the paper.
 * The sheet is {@link GuiSkin#BLUEPRINT}: it unrolls, a T-square follows the pointer over the
 * drawing, a picked part is traced round and then dashed.
 */
public class GunStandScreen extends SkinnedScreen<GunStandMenu> {

    private static final int W = 400, H = 258;
    /** The type selector, in the header. */
    private static final int TYPE_L_X = 296, TYPE_R_X = 378;
    /** The specification column: a row per setting of the picked part. */
    private static final int ROW_Y = 127, ROW_H = 15, MINUS_X = 78, PLUS_X = 180, COL_X = 200;
    /** The figures column, and the title block under it. */
    private static final int FIG_X = 206, FIG_Y = 126, FIG_W = 184, BLOCK_Y = 212, SHEET_X0 = 296, SHEET_X1 = 330;
    /** The drawing, relative to the screen; the gun is drawn relative to it. */
    private static final int BP_X = 8, BP_Y = 19, BP_W = 384, BP_H = 88;
    /** The gun is laid out on a 240-wide sheet; this centres it in the drawing. */
    private static final int GUN_DX = (BP_W - 240) / 2;
    /** Where the slide sits; the barrel runs left from its front, the grip hangs from its back. */
    private static final int LONG_X0 = 96, SLIDE_X0 = 120, SLIDE_X1 = 176, SLIDE_Y0 = 14, SLIDE_Y1 = 28, BORE_Y = 21;
    private static final int PAPER = 0xFF1B4C86, LINE = 0xFFE8F1FF, RULE = 0x60E8F1FF, PICKED = 0xFFFFE070,
            PICKED_DASH = 0x90FFE070, STAMP = 0xE85A4F;
    private static final float TRACE_MS = 180, FLASH_MS = 450;

    private record Box(int x0, int y0, int x1, int y1) {
        boolean has(double x, double y) {
            return x >= x0 && x < x1 && y >= y0 && y < y1;
        }
    }

    /** One setting of the selected part: a name, how its value reads, and a step either way. */
    private record Row(String name, Function<GunDesign, Component> value, UnaryOperator<GunDesign> down,
                       UnaryOperator<GunDesign> up) {
    }

    private GunDesign design;
    /** The design's name, typed in the title block; it goes onto the blueprint and every gun built from it. */
    private String name;
    private EditBox nameBox;
    private Part part = Part.BARREL;
    /** The sample last seen, so only a change of sample paints the selected part; null until the first tick. */
    private ItemStack lastSample;

    /** When the picked part was picked, for tracing its outline. */
    private long pickedAt = Util.getMillis();
    /** Each part's hover highlight, easing in and out. */
    private final Map<Part, Float> hover = new EnumMap<>(Part.class);
    /** Each row's value as last drawn and when it last changed, so a change flashes. */
    private final String[] shown = new String[4];
    private final long[] changedAt = new long[4];
    /** The chamber gauge as drawn, easing toward the figure. */
    private float gauge = -1;
    private long lastFrame, stampedAt = -10_000;

    public GunStandScreen(GunStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.BLUEPRINT);
        this.design = menu.opened();
        this.name = menu.openedName();
        imageWidth = W;
        imageHeight = H;
    }

    private static <E extends Enum<E>> E step(E[] values, E e, int by) {
        return values[Mth.clamp(e.ordinal() + by, 0, values.length - 1)];
    }

    /** Open, suppressed or braked: the muzzle's three states. */
    private static final List<java.util.Optional<GunDesign.Fitting>> MUZZLES = List.of(java.util.Optional.empty(),
            java.util.Optional.of(GunDesign.Fitting.SUPPRESSOR), java.util.Optional.of(GunDesign.Fitting.BRAKE));

    private static java.util.Optional<GunDesign.Fitting> muzzle(GunDesign d) {
        return d.fittings().keySet().stream().filter(GunDesign.Fitting::muzzle).findFirst();
    }

    private static Component muzzleName(GunDesign d) {
        return muzzle(d).<Component>map(f -> Component.translatable(f.langKey()))
                .orElse(Component.translatable("gui.feedback.gun_stand.open"));
    }

    /** A fitting on paper is in the barrel's metal; the part assembled into it decides what it really is. */
    private static GunDesign stepMuzzle(GunDesign d, int by) {
        int i = Mth.clamp(MUZZLES.indexOf(muzzle(d)) + by, 0, MUZZLES.size() - 1);
        GunDesign bare = muzzle(d).map(d::without).orElse(d);
        return MUZZLES.get(i).map(f -> bare.withFitting(f, d.material())).orElse(bare);
    }

    private static Row toggle(String name, GunDesign.Fitting f) {
        return new Row(name, d -> Component.translatable(d.has(f) ? "gui.feedback.gun_stand.fitted" : "gui.feedback.gun_stand.none"),
                d -> d.without(f), d -> d.withFitting(f, d.material()));
    }

    /** The feed, and what it holds when the rounds live in the gun. */
    private static Component feedName(GunDesign d) {
        Component name = Component.translatable("gun.feedback.feed." + d.layout().feed().getSerializedName());
        return d.internalCapacity().isEmpty() ? name
                : Component.translatable("gun.feedback.feed.rounds", name, d.internalCapacity().getAsInt());
    }

    private static GunLayout.Feed stepFeed(GunDesign d, int by) {
        List<GunLayout.Feed> offered = d.layout().type().feeds(d.layout().action());
        return offered.get(Mth.clamp(offered.indexOf(d.layout().feed()) + by, 0, offered.size() - 1));
    }

    /** The next action along among the ones the type offers. */
    private static GunLayout.Action stepAction(GunDesign d, int by) {
        List<GunLayout.Action> offered = d.layout().type().actions();
        return offered.get(Mth.clamp(offered.indexOf(d.layout().action()) + by, 0, offered.size() - 1));
    }

    private List<Row> rows() {
        return switch (part) {
            case BARREL -> List.of(
                    new Row("bore", d -> Component.literal(d.boreMm() + " mm"),
                            d -> d.withBore(d.boreMm() - 1), d -> d.withBore(d.boreMm() + 1)),
                    new Row("barrel", d -> Component.translatable("gun.feedback.barrel." + d.barrel().getSerializedName()),
                            d -> d.withBarrel(step(Barrel.values(), d.barrel(), -1)), d -> d.withBarrel(step(Barrel.values(), d.barrel(), 1))),
                    new Row("wall", d -> Component.translatable("gun.feedback.wall." + d.wall().getSerializedName()),
                            d -> d.withWall(step(Wall.values(), d.wall(), -1)), d -> d.withWall(step(Wall.values(), d.wall(), 1))),
                    new Row("muzzle", GunStandScreen::muzzleName, d -> stepMuzzle(d, -1), d -> stepMuzzle(d, 1)));
            // The sight mounts on the receiver (a pistol's frame), whatever the action works.
            case RECEIVER -> List.of(
                    new Row("sight", d -> Component.translatable("gun.feedback.sight." + d.sight().getSerializedName()),
                            d -> d.withSight(step(Sight.values(), d.sight(), -1)), d -> d.withSight(step(Sight.values(), d.sight(), 1))),
                    toggle("vent", GunDesign.Fitting.VENT),
                    toggle("shroud", GunDesign.Fitting.SHROUD));
            // The launcher is the one fork that changes what the gun is. Shapes of a chamber gun
            // (carbine, revolver, shotgun) are a later pass and will be options of their own.
            case TRIGGER -> List.of(
                    new Row("action", d -> Component.translatable("gun.feedback.action." + d.layout().action().getSerializedName()),
                            d -> d.withLayout(d.layout().withAction(stepAction(d, -1))),
                            d -> d.withLayout(d.layout().withAction(stepAction(d, 1)))),
                    new Row("feed", GunStandScreen::feedName,
                            d -> d.withLayout(d.layout().withFeed(stepFeed(d, -1))),
                            d -> d.withLayout(d.layout().withFeed(stepFeed(d, 1)))),
                    new Row("launcher", d -> Component.translatable("gun.feedback.launcher." + d.launcher().getSerializedName()),
                            d -> d.withLauncher(Launcher.CHAMBER), d -> d.withLauncher(Launcher.COIL)));
            case GRIP, SLIDE -> List.of();
        };
    }

    @Override
    protected void init() {
        super.init();
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = topPos + ROW_Y + i * ROW_H;
            addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> edit(row.down()), leftPos + MINUS_X, y, 14, 12,
                    FSounds.UI_DETENT::get, 0.9f));
            addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> edit(row.up()), leftPos + PLUS_X, y, 14, 12,
                    FSounds.UI_DETENT::get, 1.1f));
        }
        // The type is the first choice (spec 12), so it sits over everything, in the header.
        addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> edit(d -> d.withLayout(d.layout().withType(
                        step(GunLayout.Type.values(), d.layout().type(), -1)))), leftPos + TYPE_L_X, topPos + 4, 14, 12,
                () -> SoundEvents.BOOK_PAGE_TURN, 0.9f));
        addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> edit(d -> d.withLayout(d.layout().withType(
                        step(GunLayout.Type.values(), d.layout().type(), 1)))), leftPos + TYPE_R_X, topPos + 4, 14, 12,
                () -> SoundEvents.BOOK_PAGE_TURN, 1.1f));
        nameBox = new EditBox(font, leftPos + COL_X + 5, topPos + BLOCK_Y + 19, SHEET_X0 - COL_X - 10, 10,
                Component.translatable("gui.feedback.gun_stand.name"));
        nameBox.setBordered(false);
        nameBox.setTextColor(skin.ink());
        nameBox.setMaxLength(GunStandBlockEntity.NAME_MAX);
        nameBox.setHint(Component.translatable("gui.feedback.gun_stand.name").withColor(skin.dim()));
        nameBox.setValue(name);
        nameBox.setResponder(v -> {
            name = v;
            send(false);
        });
        addRenderableWidget(nameBox);
        addRenderableWidget(GuiStyle.primary(skin, upper("gui.feedback.gun_stand.print"), b -> print(),
                leftPos + SHEET_X1 + 6, topPos + BLOCK_Y + 14, W - SHEET_X1 - 18, 20, FSounds.UI_STAMP::get));
    }

    /** Print, and stamp the title block if there is paper to print on and a gun to print. */
    private void print() {
        send(true);
        if (!menu.getSlot(0).getItem().isEmpty() && design.bake(ClientMaterials.on(minecraft.level)).isPresent())
            stampedAt = Util.getMillis();
    }

    private static Component upper(String key) {
        return Component.literal(Component.translatable(key).getString().toUpperCase(Locale.ROOT));
    }

    /** A sample put in sets the selected part's metal; switching parts with it still in does not. */
    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack sample = menu.getSlot(1).getItem();
        if (lastSample != null && !ItemStack.isSameItem(sample, lastSample))
            GunDesign.sampleMaterial(ClientMaterials.on(minecraft.level), sample, part)
                    .filter(m -> !m.equals(design.material(part)))
                    .ifPresent(m -> edit(d -> d.withMaterial(part, m)));
        lastSample = sample.copy();
    }

    private void edit(UnaryOperator<GunDesign> change) {
        design = change.apply(design);
        send(false);
        // A new type or action can take away the part that was picked (a break action has no bolt).
        if (!boxes().containsKey(part))
            pick(Part.BARREL);
    }

    private void pick(Part p) {
        part = p;
        pickedAt = Util.getMillis();
        java.util.Arrays.fill(shown, null);
        rebuildWidgets();
    }

    private void send(boolean print) {
        PacketDistributor.sendToServer(new GunStandPayload(menu.getPos(), design, name, print));
    }

    /** Whether the design is drawn as a long gun: a receiver and a stock rather than a pistol's slide and grip. */
    private boolean longGun() {
        return design.layout().type() != GunLayout.Type.PISTOL;
    }

    /** Each part's outline on the blueprint, relative to it, as this design draws it. A part it does not have is absent. */
    private Map<Part, Box> boxes() {
        boolean rifle = longGun();
        int length = switch (design.barrel()) {
            case SHORT -> rifle ? 40 : 30;
            case MEDIUM -> rifle ? 60 : 50;
            case LONG -> rifle ? 90 : 80;
        };
        int half = 3 + design.wall().ordinal();
        Map<Part, Box> out = new EnumMap<>(Part.class);
        if (rifle) {
            out.put(Part.BARREL, new Box(LONG_X0 - length, BORE_Y - half, LONG_X0, BORE_Y + half));
            out.put(Part.RECEIVER, new Box(LONG_X0, 14, 150, 30));
            if (design.actionPart().isPresent())
                out.put(Part.SLIDE, new Box(118, 9, 136, 14));
            out.put(Part.TRIGGER, new Box(104, 30, 124, 42));
            out.put(Part.GRIP, new Box(150, 14, 214, 32));
        } else {
            out.put(Part.BARREL, new Box(SLIDE_X0 - length, BORE_Y - half, SLIDE_X0, BORE_Y + half));
            if (design.actionPart().isPresent())
                out.put(Part.SLIDE, new Box(SLIDE_X0, SLIDE_Y0, SLIDE_X1, SLIDE_Y1));
            out.put(Part.RECEIVER, new Box(124, SLIDE_Y1, SLIDE_X1, SLIDE_Y1 + 6));
            out.put(Part.TRIGGER, new Box(128, 34, 150, 46));
            out.put(Part.GRIP, new Box(156, 34, 174, 66));
        }
        return out;
    }

    private Part partAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos - BP_X - GUN_DX, y = mouseY - topPos - BP_Y;
        for (var e : boxes().entrySet())
            if (e.getValue().has(x, y))
                return e.getKey();
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Part clicked = button == 0 ? partAt(mouseX, mouseY) : null;
        if (clicked == null)
            return super.mouseClicked(mouseX, mouseY, button);
        if (!settled())
            return true;
        if (clicked != part) {
            pick(clicked);
            GuiSkin.play(skin.click(), 1f);
        }
        return true;
    }

    private void slotBox(GuiGraphics g, int x, int y) {
        skin.slot(g, leftPos + x, topPos + y);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        long now = Util.getMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(now - lastFrame, 100L) / 1000f;
        lastFrame = now;
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int x = leftPos, y = topPos;
        g.fill(x + 6, y + 110, x + W - 6, y + 111, RULE);
        g.fill(x + COL_X, y + 111, x + COL_X + 1, y + H - 6, RULE);
        g.fill(x + COL_X + 1, y + BLOCK_Y, x + W - 6, y + BLOCK_Y + 1, RULE);
        g.fill(x + SHEET_X0, y + BLOCK_Y + 1, x + SHEET_X0 + 1, y + H - 6, RULE);
        g.fill(x + SHEET_X1, y + BLOCK_Y + 1, x + SHEET_X1 + 1, y + H - 6, RULE);
        g.fill(x + COL_X + 5, y + BLOCK_Y + 30, x + SHEET_X0 - 5, y + BLOCK_Y + 31, RULE); // the name's line
        slotBox(g, GunStandMenu.PAPER_X, GunStandMenu.PAPER_Y);
        slotBox(g, GunStandMenu.SAMPLE_X, GunStandMenu.SAMPLE_Y);
        for (int c = 0; c < 9; c++)
            slotBox(g, GunStandMenu.HOTBAR_X + c * 18, GunStandMenu.HOTBAR_Y);
        Part hovered = settled() ? partAt(mouseX, mouseY) : null;
        for (Part p : Part.values())
            hover.merge(p, 0f, (h, z) -> h + Mth.clamp((p == hovered ? 1f : 0f) - h, -dt * 8, dt * 8));
        blueprint(g, now, mouseX, mouseY);
        chamberGauge(g, dt);
    }

    private void blueprint(GuiGraphics g, long now, int mouseX, int mouseY) {
        int sx = leftPos + BP_X, ox = sx + GUN_DX, oy = topPos + BP_Y;
        g.renderOutline(sx, oy, BP_W, BP_H, 0x70E8F1FF);
        // A ruler along the top and left edges, marked where the pointer is.
        for (int x = 6; x < BP_W; x += 6)
            g.fill(sx + x, oy + 1, sx + x + 1, oy + 1 + (x % 24 == 0 ? 4 : 2), RULE);
        for (int y = 6; y < BP_H; y += 6)
            g.fill(sx + 1, oy + y, sx + 1 + (y % 24 == 0 ? 4 : 2), oy + y + 1, RULE);
        boolean over = settled() && mouseX > sx && mouseX < sx + BP_W - 1 && mouseY > oy && mouseY < oy + BP_H - 1;
        if (over) { // the T-square and set-square, following the pointer
            g.fill(sx + 1, mouseY, sx + BP_W - 1, mouseY + 1, 0x22FFFFFF);
            g.fill(mouseX, oy + 1, mouseX + 1, oy + BP_H - 1, 0x22FFFFFF);
            g.fill(mouseX, oy + 1, mouseX + 1, oy + 7, PICKED);
            g.fill(sx + 1, mouseY, sx + 7, mouseY + 1, PICKED);
        }

        Map<Part, Box> boxes = boxes();
        for (var e : boxes.entrySet()) {
            Box b = e.getValue();
            int x0 = ox + b.x0(), y0 = oy + b.y0(), x1 = ox + b.x1(), y1 = oy + b.y1();
            float h = hover.getOrDefault(e.getKey(), 0f);
            if (e.getKey() == part)
                g.fill(x0, y0, x1, y1, 0x30FFE070);
            else if (h > 0)
                g.fill(x0, y0, x1, y1, (int) (h * 0x30) << 24 | 0xFFFFFF);
            g.renderOutline(x0, y0, x1 - x0, y1 - y0, LINE);
            if (e.getKey() != part)
                continue;
            // Picked: traced round in the accent, then dashed just outside, the dashes marching.
            int perimeter = 2 * (x1 - x0 + y1 - y0 - 2);
            float t = (now - pickedAt) / TRACE_MS;
            GuiStyle.perimeter(g, x0, y0, x1, y1, 0, Math.round(perimeter * GuiStyle.easeOut(t)), PICKED);
            if (t >= 1) {
                int ring = 2 * (x1 - x0 + y1 - y0 + 6), march = (int) (now / 70 % 6);
                for (int d = 0; d < ring; d += 6)
                    GuiStyle.perimeter(g, x0 - 2, y0 - 2, x1 + 2, y1 + 2, d + march, 3, PICKED_DASH);
            }
        }

        Box barrel = boxes.get(Part.BARREL);
        // The bore at the muzzle, as wide as it is against the wall around it.
        int bore = Math.max(1, Math.round(design.boreMm() / 20f * (barrel.y1() - barrel.y0() - 2)));
        g.fill(ox + barrel.x0(), oy + BORE_Y - bore / 2, ox + barrel.x0() + 2, oy + BORE_Y - bore / 2 + bore, PAPER);
        if (design.launcher() == Launcher.COIL)
            for (int x = barrel.x0() + 4; x < barrel.x1() - 1; x += 4)
                g.vLine(ox + x, oy + barrel.y0(), oy + barrel.y1() - 1, LINE);
        if (longGun())
            longGunDetail(g, ox, oy, barrel);
        else
            pistolDetail(g, ox, oy, barrel);
        Component hint = Component.translatable("gui.feedback.gun_stand.pick");
        g.drawString(font, hint, sx + BP_W - 5 - font.width(hint), oy + BP_H - 11, skin.dim(), false);
        g.drawString(font, upper("gui.feedback.gun_stand.not_to_scale"), sx + 9, oy + BP_H - 11, skin.dim(), false);
    }

    /** The chamber's charge against the metal's rating, as a bar: the rating is the tick, past it is red. */
    private void chamberGauge(GuiGraphics g, float dt) {
        var figures = design.bake(ClientMaterials.on(minecraft.level));
        if (figures.isEmpty())
            return;
        float target = figures.get().overpressure();
        gauge = gauge < 0 ? target : gauge + (target - gauge) * Math.min(1, dt * 10);
        int x = leftPos + FIG_X, y = topPos + gaugeY(), w = FIG_W;
        int fill = Math.round(Math.min(gauge, 1.5f) / 1.5f * w), rating = w * 2 / 3;
        g.fill(x, y, x + w, y + 4, 0x30000000);
        g.renderOutline(x - 1, y - 1, w + 2, 6, RULE);
        g.fill(x, y, x + Math.min(fill, rating), y + 4, 0xC0E8F1FF);
        if (fill > rating)
            g.fill(x + rating, y, x + fill, y + 4, skin.warn());
        g.fill(x + rating, y - 2, x + rating + 1, y + 6, PICKED);
    }

    /** Where the gauge sits: under the chamber line, however many lines it wrapped to. */
    private int gaugeY() {
        var figures = design.bake(ClientMaterials.on(minecraft.level));
        if (figures.isEmpty())
            return FIG_Y;
        return FIG_Y + 9 * font.split(GunTooltip.figures(figures.get()).getFirst(), FIG_W).size() + 1;
    }

    /** The barrel's label, centred over it, wherever it ends. */
    private void barrelLabel(GuiGraphics g, int ox, int oy, Box barrel) {
        int mid = (barrel.x0() + barrel.x1()) / 2;
        label(g, Part.BARREL, mid - font.width(partName(Part.BARREL)) / 2, 2);
        g.vLine(ox + mid, oy + 10, oy + barrel.y0(), LINE);
    }

    private void pistolDetail(GuiGraphics g, int ox, int oy, Box barrel) {
        switch (design.sight()) {
            case IRON -> {
                g.fill(ox + 122, oy + 11, ox + 126, oy + SLIDE_Y0, LINE);
                g.fill(ox + 172, oy + 10, ox + 174, oy + SLIDE_Y0, LINE);
            }
            case REFLEX -> g.renderOutline(ox + 146, oy + 6, 14, 8, LINE);
            case SCOPE -> {
                g.renderOutline(ox + 128, oy + 6, 40, 6, LINE);
                g.vLine(ox + 136, oy + 11, oy + SLIDE_Y0, LINE);
                g.vLine(ox + 160, oy + 11, oy + SLIDE_Y0, LINE);
            }
        }
        g.vLine(ox + 138, oy + 34, oy + 42, LINE); // trigger blade
        switch (design.layout().feed()) {
            case CYLINDER -> g.renderOutline(ox + 104, oy + 12, 16, 18, LINE);
            case BOX -> g.hLine(ox + 156, ox + 173, oy + 62, LINE); // the magazine's base in the grip
            default -> {
            }
        }

        barrelLabel(g, ox, oy, barrel);
        if (design.actionPart().isPresent()) {
            label(g, Part.SLIDE, 190, 4);
            g.hLine(ox + 170, ox + 188, oy + 8, LINE);
            g.vLine(ox + 170, oy + 8, oy + SLIDE_Y0, LINE);
        }
        label(g, Part.RECEIVER, 190, 26);
        g.hLine(ox + SLIDE_X1, ox + 188, oy + 30, LINE);
        label(g, Part.GRIP, 190, 46);
        g.hLine(ox + 174, ox + 188, oy + 50, LINE);
        int triggerEnd = 70 + font.width(partName(Part.TRIGGER)) + 2;
        label(g, Part.TRIGGER, 70, 70);
        g.hLine(ox + triggerEnd, ox + 139, oy + 74, LINE);
        g.vLine(ox + 139, oy + 46, oy + 74, LINE);
    }

    private void longGunDetail(GuiGraphics g, int ox, int oy, Box barrel) {
        switch (design.sight()) {
            case IRON -> {
                g.fill(ox + barrel.x0() + 2, oy + barrel.y0() - 3, ox + barrel.x0() + 4, oy + barrel.y0(), LINE);
                g.fill(ox + 140, oy + 11, ox + 144, oy + 14, LINE);
            }
            case REFLEX -> g.renderOutline(ox + 138, oy + 5, 10, 9, LINE);
            case SCOPE -> {
                g.renderOutline(ox + 98, oy + 3, 50, 6, LINE);
                g.vLine(ox + 102, oy + 8, oy + 14, LINE);
                g.vLine(ox + 112, oy + 8, oy + 14, LINE);
            }
        }
        g.vLine(ox + 114, oy + 30, oy + 38, LINE); // trigger blade
        g.renderOutline(ox + 150, oy + 30, 14, 14, LINE); // the stock's wrist
        switch (design.layout().feed()) {
            case TUBE -> g.renderOutline(ox + barrel.x0() + 6, oy + barrel.y1(), LONG_X0 - barrel.x0() - 6, 4, LINE);
            case BOX -> g.renderOutline(ox + 128, oy + 30, 12, 20, LINE);
            case BELT -> {
                g.renderOutline(ox + 126, oy + 30, 20, 16, LINE);
                for (int x = 128; x < 146; x += 3)
                    g.vLine(ox + x, oy + 46, oy + 50, LINE);
            }
            case CYLINDER -> g.renderOutline(ox + 82, oy + 12, 14, 18, LINE);
            case BARRELS -> {
                if (design.internalCapacity().orElse(1) > 1) // a double: the second barrel under the first
                    g.renderOutline(ox + barrel.x0(), oy + barrel.y1(), barrel.x1() - barrel.x0(), barrel.y1() - barrel.y0(), LINE);
                g.renderOutline(ox + LONG_X0 - 2, oy + 28, 4, 4, LINE); // the hinge
            }
        }
        if (design.layout().type() == GunLayout.Type.LMG) { // a bipod near the muzzle
            int x = barrel.x0() + 10;
            g.vLine(ox + x, oy + barrel.y1(), oy + 62, LINE);
            g.vLine(ox + x + 6, oy + barrel.y1(), oy + 62, LINE);
        }
        if (design.layout().type() == GunLayout.Type.SHOTGUN && design.layout().action() == GunLayout.Action.PUMP)
            g.renderOutline(ox + barrel.x0() + 14, oy + barrel.y1() - 1, 24, 7, LINE); // the fore-end

        barrelLabel(g, ox, oy, barrel);
        if (design.actionPart().isPresent()) {
            label(g, Part.SLIDE, 162, 4);
            g.hLine(ox + 136, ox + 160, oy + 11, LINE);
        }
        label(g, Part.RECEIVER, 150, 54);
        g.vLine(ox + 120, oy + 30, oy + 58, LINE);
        g.hLine(ox + 120, ox + 148, oy + 58, LINE);
        label(g, Part.GRIP, 196, 38);
        int triggerEnd = 40 + font.width(partName(Part.TRIGGER)) + 2;
        label(g, Part.TRIGGER, 40, 70);
        g.hLine(ox + triggerEnd, ox + 110, oy + 74, LINE);
        g.vLine(ox + 110, oy + 42, oy + 74, LINE);
    }

    /** What a part is called on this gun: the action's part is a slide or a bolt, a long gun's grip a stock. */
    private Component partName(Part p) {
        if (p == Part.SLIDE)
            return Component.translatable("gun.feedback.part." + design.actionPart().orElse("slide"));
        if (p == Part.GRIP && longGun())
            return Component.translatable("gun.feedback.part.stock");
        return Component.translatable(p.langKey());
    }

    private void label(GuiGraphics g, Part p, int x, int y) {
        g.drawString(font, partName(p), leftPos + BP_X + GUN_DX + x, topPos + BP_Y + y,
                p == part ? PICKED : LINE, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        long now = Util.getMillis();
        g.drawString(font, Component.literal(title.getString().toUpperCase(Locale.ROOT)), 12, 7, skin.ink(), false);
        g.drawCenteredString(font, Component.translatable("gun.feedback.type." + design.layout().type().getSerializedName()),
                (TYPE_L_X + 14 + TYPE_R_X) / 2, 6, skin.accent());

        // Column A: the picked part's specification.
        Component heading = upper("gui.feedback.gun_stand.spec").copy().append(" \u00b7 ");
        g.drawString(font, heading, 12, 114, skin.dim(), false);
        g.drawString(font, Component.literal(partName(part).getString().toUpperCase(Locale.ROOT)),
                12 + font.width(heading), 114, skin.accent(), false);
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            int y = ROW_Y + i * ROW_H + 2;
            String value = rows.get(i).value().apply(design).getString();
            if (shown[i] != null && !shown[i].equals(value))
                changedAt[i] = now;
            shown[i] = value;
            float flash = Math.max(0, 1 - (now - changedAt[i]) / FLASH_MS);
            int colour = flash > 0 ? lerp(skin.ink(), skin.accent(), flash) : skin.ink();
            g.drawString(font, Component.translatable("gui.feedback.gun_stand." + rows.get(i).name()), 12, y, skin.dim(), false);
            g.drawCenteredString(font, value, (MINUS_X + 14 + PLUS_X) / 2, y, colour);
        }
        if (rows.isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.gun_stand.metal_only"), 12, ROW_Y + 2, skin.dim(), false);
        // The material sits beside its sample slot, and is the selected part's.
        int y = GunStandMenu.SAMPLE_Y + 4;
        g.drawString(font, Component.translatable("gui.feedback.gun_stand.material"), 12, y, skin.dim(), false);
        g.drawString(font, GunTooltip.name(design.material(part)), GunStandMenu.SAMPLE_X + 22, y, skin.ink(), false);

        // Column B: the figures, the chamber gauge under the first line.
        g.drawString(font, upper("gui.feedback.gun_stand.figures"), FIG_X, 114, skin.dim(), false);
        var figures = design.bake(ClientMaterials.on(minecraft.level));
        if (figures.isEmpty()) {
            for (var l : font.split(Component.translatable("gun.feedback.unknown_material"), FIG_W))
                g.drawString(font, l, FIG_X, FIG_Y, skin.warn(), false);
        } else {
            y = FIG_Y;
            boolean first = true;
            for (Component line : GunTooltip.figures(figures.get())) {
                // Red in the tooltip is overpressure; here it is the sheet's warning colour, the rest ink.
                int colour = line.getStyle().getColor() != null
                        && line.getStyle().getColor().getValue() == ChatFormatting.RED.getColor() ? skin.warn() : skin.ink();
                for (var l : font.split(Component.literal(line.getString()), FIG_W)) {
                    g.drawString(font, l, FIG_X, y, colour, false);
                    y += 9;
                }
                if (first)
                    y += 7; // room for the gauge
                first = false;
            }
            for (var l : font.split(Component.literal(GunTooltip.assembly(design).getString()), FIG_W)) {
                if (y + 9 > BLOCK_Y)
                    break;
                g.drawString(font, l, FIG_X, y, skin.dim(), false);
                y += 9;
            }
        }

        // The title block.
        g.drawString(font, upper("gui.feedback.gun_stand.design"), COL_X + 5, BLOCK_Y + 5, skin.dim(), false);
        g.drawString(font, upper("gui.feedback.gun_stand.sheet"), SHEET_X0 + 4, BLOCK_Y + 5, skin.dim(), false);
        stamp(g, now);
    }

    private static int lerp(int a, int b, float t) {
        return net.minecraft.util.FastColor.ARGB32.lerp(t, a, b);
    }

    /** PRINTED, stamped across the title block: lands big, settles, then fades. */
    private void stamp(GuiGraphics g, long now) {
        long age = now - stampedAt;
        if (age > 1600)
            return;
        float scale = 1 + 0.8f * (1 - GuiStyle.easeOut(Math.min(1, age / 110f)));
        int alpha = (int) (255 * Mth.clamp(1 - (age - 1200) / 400f, 0, 1));
        if (alpha < 8)
            return;
        Component text = upper("gui.feedback.gun_stand.printed");
        int w = font.width(text) + 10, h = 15, colour = alpha << 24 | STAMP;
        var pose = g.pose();
        pose.pushPose();
        pose.translate((COL_X + SHEET_X0) / 2f, BLOCK_Y + 18, 300);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-10));
        pose.scale(scale, scale, 1);
        g.renderOutline(-w / 2, -h / 2, w, h, colour);
        g.renderOutline(-w / 2 + 2, -h / 2 + 2, w - 4, h - 4, colour);
        g.drawString(font, text, -font.width(text) / 2, -3, colour, false);
        pose.popPose();
    }

    /** Typing goes to the name box while it has focus, so E does not close the screen mid-word. */
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE && nameBox != null && nameBox.isFocused())
            return nameBox.keyPressed(key, scan, modifiers) || nameBox.canConsumeInput() || super.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }
}
