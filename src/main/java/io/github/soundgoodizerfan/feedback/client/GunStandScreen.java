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
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import io.github.soundgoodizerfan.feedback.weapon.GunStandBlockEntity;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The Gun Stand's editor ({@code feedback_guns_spec.md} section 4), drawn as a blueprint: the gun
 * on a grid, its parts labelled, and the drawing reshaping as the design does -- a long barrel
 * draws long, a thick wall thick, a coil wound. Click a part and its settings show below; the
 * sample slot sets the selected part's metal (spec 2: a gun is parts, each with a material). The
 * figures the design makes read live off the material system, the chamber in red when the metal
 * is asked for too much, so a bad gun is seen on the Stand and not found by firing it. Every edit
 * goes to the stand; Print writes it onto the paper. Flat fills -- all art is placeholder.
 */
public class GunStandScreen extends AbstractContainerScreen<GunStandMenu> {

    private static final int TYPE_X = 140, ROW_Y = 108, ROW_H = 16, MINUS_X = 70, PLUS_X = 144, FIGURES_Y = 180;
    /** The blueprint, relative to the screen; the gun is drawn relative to it. */
    private static final int BP_X = 8, BP_Y = 18, BP_W = 304, BP_H = 86, GRID = 12;
    /** The gun is laid out on a 240-wide sheet; this centres it on the wider blueprint. */
    private static final int GUN_DX = (BP_W - 240) / 2;
    /** Where the slide sits; the barrel runs left from its front, the grip hangs from its back. */
    private static final int LONG_X0 = 96, SLIDE_X0 = 120, SLIDE_X1 = 176, SLIDE_Y0 = 14, SLIDE_Y1 = 28, BORE_Y = 21;
    private static final int PAPER = 0xFF1C5FA8, LINE = 0xFFFFFFFF, FAINT = 0x33FFFFFF, HOVER = 0x30FFFFFF,
            PICKED = 0xFFFFE070, PICKED_FILL = 0x40FFE070;

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
    /** The design's name, typed in the box over Print; it goes onto the blueprint and every gun built from it. */
    private String name;
    private EditBox nameBox;
    private Part part = Part.BARREL;
    /** The sample last seen, so only a change of sample paints the selected part; null until the first tick. */
    private ItemStack lastSample;

    public GunStandScreen(GunStandMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.design = menu.opened();
        this.name = menu.openedName();
        imageWidth = 320;
        imageHeight = 262;
        inventoryLabelY = 10_000; // no inventory label: only the hotbar is shown
    }

    private static <E extends Enum<E>> E step(E[] values, E e, int by) {
        return values[Mth.clamp(e.ordinal() + by, 0, values.length - 1)];
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
                            d -> d.withWall(step(Wall.values(), d.wall(), -1)), d -> d.withWall(step(Wall.values(), d.wall(), 1))));
            // The sight mounts on the receiver (a pistol's frame), whatever the action works.
            case RECEIVER -> List.of(
                    new Row("sight", d -> Component.translatable("gun.feedback.sight." + d.sight().getSerializedName()),
                            d -> d.withSight(step(Sight.values(), d.sight(), -1)), d -> d.withSight(step(Sight.values(), d.sight(), 1))));
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
            addRenderableWidget(Button.builder(Component.literal("<"), b -> edit(row.down())).bounds(leftPos + MINUS_X, y, 16, 14).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> edit(row.up())).bounds(leftPos + PLUS_X, y, 16, 14).build());
        }
        // The type is the first choice (spec 12), so it sits over everything, by the title.
        addRenderableWidget(Button.builder(Component.literal("<"), b -> edit(d -> d.withLayout(d.layout().withType(
                step(GunLayout.Type.values(), d.layout().type(), -1))))).bounds(leftPos + TYPE_X, topPos + 2, 14, 14).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> edit(d -> d.withLayout(d.layout().withType(
                step(GunLayout.Type.values(), d.layout().type(), 1))))).bounds(leftPos + imageWidth - 22, topPos + 2, 14, 14).build());
        nameBox = new EditBox(font, leftPos + GunStandMenu.PAPER_X, topPos + ROW_Y, imageWidth - GunStandMenu.PAPER_X - 8, 14,
                Component.translatable("gui.feedback.gun_stand.name"));
        nameBox.setMaxLength(GunStandBlockEntity.NAME_MAX);
        nameBox.setHint(Component.translatable("gui.feedback.gun_stand.name").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(name);
        nameBox.setResponder(v -> {
            name = v;
            send(false);
        });
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.feedback.gun_stand.print"), b -> send(true))
                .bounds(leftPos + GunStandMenu.PAPER_X + 20, topPos + GunStandMenu.PAPER_Y, 56, 16).build());
    }

    /** A sample put in sets the selected part's metal; switching parts with it still in does not. */
    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack sample = menu.getSlot(1).getItem();
        if (lastSample != null && !ItemStack.isSameItem(sample, lastSample))
            GunDesign.sampleMaterial(ClientMaterials.on(minecraft.level), sample)
                    .filter(m -> !m.equals(design.material(part)))
                    .ifPresent(m -> edit(d -> d.withMaterial(part, m)));
        lastSample = sample.copy();
    }

    private void edit(UnaryOperator<GunDesign> change) {
        design = change.apply(design);
        send(false);
        // A new type or action can take away the part that was picked (a break action has no bolt).
        if (!boxes().containsKey(part)) {
            part = Part.BARREL;
            rebuildWidgets();
        }
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
        if (clicked != part) {
            part = clicked;
            rebuildWidgets();
        }
        return true;
    }

    private void slotBox(GuiGraphics g, int x, int y) {
        g.fill(leftPos + x - 1, topPos + y - 1, leftPos + x + 17, topPos + y + 17, 0xFF8B8B8B);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        slotBox(g, GunStandMenu.PAPER_X, GunStandMenu.PAPER_Y);
        slotBox(g, GunStandMenu.SAMPLE_X, GunStandMenu.SAMPLE_Y);
        for (int c = 0; c < 9; c++)
            slotBox(g, GunStandMenu.HOTBAR_X + c * 18, GunStandMenu.HOTBAR_Y);
        blueprint(g, partAt(mouseX, mouseY));
    }

    private void blueprint(GuiGraphics g, Part hovered) {
        int sx = leftPos + BP_X, ox = sx + GUN_DX, oy = topPos + BP_Y;
        g.fill(sx, oy, sx + BP_W, oy + BP_H, PAPER);
        for (int x = GRID; x < BP_W; x += GRID)
            g.vLine(sx + x, oy, oy + BP_H, FAINT);
        for (int y = GRID; y < BP_H; y += GRID)
            g.hLine(sx, sx + BP_W - 1, oy + y, FAINT);

        Map<Part, Box> boxes = boxes();
        for (var e : boxes.entrySet()) {
            Box b = e.getValue();
            if (e.getKey() == part)
                g.fill(ox + b.x0(), oy + b.y0(), ox + b.x1(), oy + b.y1(), PICKED_FILL);
            else if (e.getKey() == hovered)
                g.fill(ox + b.x0(), oy + b.y0(), ox + b.x1(), oy + b.y1(), HOVER);
            g.renderOutline(ox + b.x0(), oy + b.y0(), b.x1() - b.x0(), b.y1() - b.y0(), e.getKey() == part ? PICKED : LINE);
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
        g.drawString(font, hint, sx + BP_W - 4 - font.width(hint), oy + BP_H - 10, FAINT | 0x99000000, false);
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
        super.renderLabels(g, mouseX, mouseY);
        g.drawCenteredString(font, Component.translatable("gun.feedback.type." + design.layout().type().getSerializedName()),
                (TYPE_X + 14 + imageWidth - 22) / 2, 5, 0xFFFFFF);
        List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            int y = ROW_Y + i * ROW_H + 3;
            g.drawString(font, Component.translatable("gui.feedback.gun_stand." + rows.get(i).name()), 8, y, 0x404040, false);
            g.drawCenteredString(font, rows.get(i).value().apply(design), (MINUS_X + 16 + PLUS_X) / 2, y, 0xFFFFFF);
        }
        // The material sits beside its sample slot, and is the selected part's.
        int y = GunStandMenu.SAMPLE_Y + 4;
        g.drawString(font, Component.translatable("gui.feedback.gun_stand.material"), 8, y, 0x404040, false);
        g.drawString(font, GunTooltip.name(design.material(part)), GunStandMenu.SAMPLE_X + 20, y, 0x404040, false);

        var materials = ClientMaterials.on(minecraft.level);
        y = FIGURES_Y;
        var figures = design.bake(materials);
        if (figures.isEmpty()) {
            g.drawString(font, Component.translatable("gun.feedback.unknown_material"), 8, y, 0xAA0000, false);
            return;
        }
        // Long lines wrap rather than run off the edge of the screen.
        int width = imageWidth - 16;
        for (Component line : GunTooltip.figures(figures.get())) {
            int colour = line.getStyle().getColor() != null ? line.getStyle().getColor().getValue() : 0x404040;
            for (var part : font.split(line, width)) {
                g.drawString(font, part, 8, y, colour, false);
                y += 9;
            }
        }
        // What it is assembled from: each part machined, then put together with the blueprint.
        for (var part : font.split(GunTooltip.assembly(design), width)) {
            g.drawString(font, part, 8, y, 0x404040, false);
            y += 9;
        }
    }

    /** Typing goes to the name box while it has focus, so E does not close the screen mid-word. */
    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE)
            return super.keyPressed(key, scan, modifiers);
        if (nameBox != null && nameBox.isFocused())
            return nameBox.keyPressed(key, scan, modifiers) || nameBox.canConsumeInput() || super.keyPressed(key, scan, modifiers);
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
