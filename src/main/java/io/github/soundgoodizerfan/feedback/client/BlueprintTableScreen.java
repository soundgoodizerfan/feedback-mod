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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.modular.BlueprintTableMenu;
import io.github.soundgoodizerfan.feedback.modular.Edge;
import io.github.soundgoodizerfan.feedback.modular.ModularBlueprintItem;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularModifiers;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.net.ModularBlueprintPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Blueprint Table: draw a tool on paper and read what it bakes to. Everything it offers comes
 * from {@link ToolDesigns}, the same model the Tool Table makes things from: a type, one of its
 * forms (which roles, how many of each), its optional parts as toggles (a tip, a binding), and for
 * each role a material (a sample item in its slot names it) and, where the part has shapes, which
 * shape -- a variant, or for a twin's head which kind of head. A drawing is exactly what the Tool
 * Table would accept, and the figures are the ones of the item it would make, so a blueprint
 * cannot promise what the table cannot do (spec 6, 4.2: the blueprint predicts, instruments
 * measure). Heat and flaws are not drawn: a flaw is what forging adds.
 */
public class BlueprintTableScreen extends SkinnedScreen<BlueprintTableMenu> {

    private static final int ROW_Y = 34, ROW_H = 18, NAME_X = 8, MAT_X = 118, MAT_W = 62, SHAPE_X = 184, SHAPE_W = 52;
    private static final int BAD = 0xFFAA0000;

    private final List<ResourceLocation> types = ToolDesigns.drawable(ModularTypes.known());
    private int type;
    private int form;
    private final Set<String> extras = new LinkedHashSet<>();
    /** The first role row on the page (see {@link #visibleRows}). */
    private int rowScroll;
    private Edge edge = Edge.STANDARD;
    /** Per role letter, which of its shapes and which of its materials is drawn. */
    private final Map<String, Integer> shape = new HashMap<>();
    private final Map<String, Integer> material = new HashMap<>();
    /** The paper last seen, so only a blueprint newly put in is opened. */
    private ItemStack lastPaper = ItemStack.EMPTY;
    private ModularDesign baked;
    private Optional<ModularFigures> figures = Optional.empty();
    /** Where each figure's line was last drawn, as x, y, width, in the panel's own coordinates. */
    private final List<int[]> figureBoxes = new ArrayList<>();
    /** The first drawing of this type or blueprint, which later swaps are compared with. */
    private Optional<ModularFigures> reference = Optional.empty();
    private boolean fresh = true;
    private List<Component> changes = List.of();
    /** Where the changes line and the modifiers line were last drawn, as x, y, width. */
    private int[] changeBox, modifierBox;
    private List<ModularModifiers.Modifier> modifiers = List.of();
    /** Where each role's shape control was drawn: row, in the panel's own coordinates. */
    private final Map<String, int[]> shapeBoxes = new HashMap<>();

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.BLUEPRINT);
        imageWidth = 264;
        imageHeight = BlueprintTableMenu.H;
        inventoryLabelY = 10_000; // only the hotbar is shown
        titleLabelY = 10_000; // the type's name is the title
        selectType(0);
    }

    private ModularType current() {
        return types.isEmpty() ? null : ModularTypes.known().get(types.get(type));
    }

    private boolean edged() {
        ModularType t = current();
        return t != null && t.edged();
    }

    /** The roles on the sheet: those drawn, and the optional ones offered, in the type's order. */
    private List<String> rows() {
        ModularType t = current();
        if (t == null)
            return List.of();
        Map<String, Integer> counts = ToolDesigns.counts(t, form, extras);
        List<String> offered = t.extras();
        return t.order().stream().filter(r -> counts.containsKey(r) || offered.contains(r)).toList();
    }

    /** The rows on the page: a type with more optional parts than the table has sample slots (a gun) scrolls with the wheel. */
    private List<String> visibleRows() {
        List<String> all = rows();
        rowScroll = Math.max(0, Math.min(rowScroll, all.size() - BlueprintTableMenu.ROLE_SLOTS));
        return all.subList(rowScroll, Math.min(all.size(), rowScroll + BlueprintTableMenu.ROLE_SLOTS));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int total = rows().size();
        if (total > BlueprintTableMenu.ROLE_SLOTS && mouseX >= leftPos && mouseX < leftPos + imageWidth && mouseY >= topPos + ROW_Y
                && mouseY < topPos + ROW_Y + BlueprintTableMenu.ROLE_SLOTS * ROW_H) {
            int was = rowScroll;
            rowScroll = Math.max(0, Math.min(total - BlueprintTableMenu.ROLE_SLOTS, rowScroll - (int) Math.signum(scrollY)));
            if (rowScroll != was) {
                // The sample slots mean other roles now: send them back to the player.
                if (net.minecraft.client.Minecraft.getInstance().gameMode != null)
                    net.minecraft.client.Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, BlueprintTableMenu.BTN_CLEAR);
                rebuildWidgets();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean drawn(String role) {
        return ToolDesigns.counts(current(), form, extras).containsKey(role);
    }

    private void selectType(int index) {
        type = types.isEmpty() ? 0 : Math.floorMod(index, types.size());
        fresh = true;
        rowScroll = 0;
        form = 0;
        extras.clear();
        shape.clear();
        material.clear();
        ModularType t = current();
        if (t == null)
            return;
        t.roles().forEach((letter, role) -> {
            List<Material.Located> c = materials(letter);
            ResourceLocation fallback = t.defaults().get(letter);
            for (int i = 0; i < c.size(); i++)
                if (c.get(i).id().equals(fallback))
                    material.put(letter, i);
        });
    }

    /** Open a blueprint: its type, form and extras, and each part's shape and material where it is a choice. */
    private void open(ModularDesign design) {
        int index = types.indexOf(design.type());
        ModularType t = index < 0 ? null : ModularTypes.known().get(design.type());
        Optional<ToolDesigns.Opened> opened = t == null ? Optional.empty() : ToolDesigns.open(design, t);
        if (opened.isEmpty())
            return;
        selectType(index);
        form = opened.get().form();
        extras.addAll(opened.get().extras());
        edge = design.edge();
        design.parts().forEach(p -> {
            List<ToolTableShaping.Option> options = shapes(p.role());
            for (int i = 0; i < options.size(); i++)
                if (options.get(i).variant().name().equals(p.variant())
                        && (p.kind().isEmpty() || options.get(i).shaping().item().getPath().equals(p.kind())))
                    shape.put(p.role(), i);
            List<Material.Located> c = materials(p.role());
            for (int i = 0; i < c.size(); i++)
                if (c.get(i).id().equals(p.material()))
                    material.put(p.role(), i);
        });
    }

    private List<ToolTableShaping.Option> shapes(String role) {
        ModularType t = current();
        return t == null ? List.of() : ToolDesigns.shapes(t.roles().get(role), ModularTypes.known());
    }

    private ToolTableShaping.Option shapeOf(String role) {
        List<ToolTableShaping.Option> s = shapes(role);
        return s.isEmpty() ? null : s.get(Math.floorMod(shape.getOrDefault(role, 0), s.size()));
    }

    private List<Material.Located> materials(String role) {
        ModularType t = current();
        return t == null ? List.of() : ToolDesigns.materials(t.roles().get(role), shapeOf(role), ClientMaterials.get());
    }

    private Optional<Material.Located> choice(String role) {
        List<Material.Located> c = materials(role);
        return c.isEmpty() ? Optional.empty() : Optional.of(c.get(Math.floorMod(material.getOrDefault(role, 0), c.size())));
    }

    /** What is drawn, as a design; empty while a role has no material that fits it. */
    private Optional<ModularDesign> design() {
        ModularType t = current();
        if (t == null)
            return Optional.empty();
        Map<String, Integer> counts = ToolDesigns.counts(t, form, extras);
        List<ModularDesign.Part> parts = new ArrayList<>();
        for (String role : t.order()) {
            if (!counts.containsKey(role))
                continue;
            Optional<Material.Located> c = choice(role);
            if (c.isEmpty())
                return Optional.empty();
            parts.add(ToolDesigns.part(role, c.get(), shapeOf(role), counts.get(role)));
        }
        return Optional.of(new ModularDesign(types.get(type), parts, edged() ? edge : Edge.STANDARD));
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + 8, topPos + 4, imageWidth - 16, 12, "<", ">",
                () -> types.isEmpty() ? "" : Component.translatable("item." + types.get(type).getNamespace() + "." + types.get(type).getPath()).getString(),
                dir -> change(() -> selectType(type + dir)), false, null));
        ModularType t = current();
        if (t != null && t.forms().size() > 1) {
            addRenderableWidget(GuiStyle.stepper(skin, leftPos + 8, topPos + 18, imageWidth - 16, 12, "<", ">",
                    () -> Component.translatable("gui.feedback.blueprint_table.form", form + 1, t.forms().size()).getString(),
                    dir -> change(() -> form = Math.floorMod(form + dir, t.forms().size())), false, null));
        }
        List<String> rows = visibleRows();
        menu.shown(rows.stream().map(this::drawn).toList());
        for (int i = 0; i < rows.size(); i++) {
            String role = rows.get(i);
            int y = topPos + ROW_Y + i * ROW_H;
            if (t.extras().contains(role))
                addRenderableWidget(GuiStyle.checkbox(skin, Component.translatable("modular.feedback.role." + t.roles().get(role).name()),
                        () -> extras.contains(role), b -> change(() -> {
                            if (!extras.remove(role))
                                extras.add(role);
                        }), leftPos + NAME_X, y, 100));
            else if (!drawn(role))
                continue;
            List<ToolTableShaping.Option> shapes = shapes(role);
            if (drawn(role) && shapes.size() > 1) {
                addRenderableWidget(GuiStyle.stepper(skin, leftPos + SHAPE_X - 14, y, SHAPE_W + 28, 14, "<", ">",
                        () -> shapeOf(role) == null ? "" : shapeOf(role).label().getString(),
                        dir -> change(() -> shape.put(role, Math.floorMod(shape.getOrDefault(role, 0) + dir, shapes.size()))), false, null));
            }
        }
        // A role's material is a sample item in its slot; the edge, the last row, steps.
        if (edged()) {
            int y = topPos + ROW_Y + rows.size() * ROW_H;
            addRenderableWidget(GuiStyle.stepper(skin, leftPos + MAT_X, y, 84, 14, "<", ">",
                    () -> Component.translatable("tooltip.feedback.modular.edge." + edge.getSerializedName()).getString(),
                    dir -> change(() -> edge = edge.step(dir)), false, null));
        }
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.blueprint_table.print"),
                b -> design().ifPresent(d -> PacketDistributor.sendToServer(new ModularBlueprintPayload(d))),
                leftPos + BlueprintTableMenu.PAPER_X + 20, topPos + BlueprintTableMenu.PAPER_Y, 42, 16));
    }

    private void change(Runnable edit) {
        edit.run();
        rebuildWidgets();
    }

    /** A sample in a role's slot sets that role's material, when the role has it as a choice. */
    private void readSamples() {
        List<String> rows = visibleRows();
        for (int i = 0; i < rows.size(); i++) {
            ItemStack sample = menu.sample(i);
            if (sample.isEmpty() || !drawn(rows.get(i)))
                continue;
            Optional<Material.Located> named = Material.find(ClientMaterials.get(), sample);
            List<Material.Located> c = materials(rows.get(i));
            if (named.isEmpty() || choice(rows.get(i)).map(l -> l.id().equals(named.get().id())).orElse(false))
                continue;
            for (int k = 0; k < c.size(); k++)
                if (c.get(k).id().equals(named.get().id()))
                    material.put(rows.get(i), k);
        }
    }

    /** A blueprint put in the slot opens on the sheet. */
    @Override
    protected void containerTick() {
        super.containerTick();
        readSamples();
        ItemStack paper = menu.getSlot(0).getItem();
        if (!ItemStack.isSameItemSameComponents(paper, lastPaper)) {
            ModularDesign design = ModularStacks.design(paper);
            if (design != null)
                change(() -> open(design));
        }
        lastPaper = paper.copy();
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + BlueprintTableMenu.PAPER_X, topPos + BlueprintTableMenu.PAPER_Y);
        for (int i = 0; i < 9; i++)
            skin.slot(g, leftPos + BlueprintTableMenu.HOTBAR_X + i * 18, topPos + BlueprintTableMenu.HOTBAR_Y);
        List<String> rows = visibleRows();
        for (int i = 0; i < rows.size(); i++)
            if (drawn(rows.get(i)))
                skin.slot(g, leftPos + BlueprintTableMenu.SAMPLE_X, topPos + BlueprintTableMenu.SAMPLE_Y + i * BlueprintTableMenu.SAMPLE_STEP);
    }

    /** {@code text} cut with an ellipsis to {@code w}. */
    private void fit(GuiGraphics g, Component text, int x, int y, int w, int colour) {
        String s = text.getString();
        g.drawString(font, font.width(s) <= w ? s : font.plainSubstrByWidth(s, w - font.width("...")) + "...", x, y, colour, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ModularType t = current();
        List<String> rows = visibleRows();
        Map<String, Integer> counts = t == null ? Map.of() : ToolDesigns.counts(t, form, extras);
        shapeBoxes.clear();
        for (int i = 0; i < rows.size(); i++) {
            String role = rows.get(i);
            int y = ROW_Y + i * ROW_H + 3;
            ModularType.Role r = t.roles().get(role);
            boolean on = counts.containsKey(role);
            if (!t.extras().contains(role)) {
                int n = counts.getOrDefault(role, 1);
                Component name = Component.translatable("modular.feedback.role." + r.name());
                g.drawString(font, n > 1 && !ToolDesigns.shaped(r) ? Component.literal(name.getString() + " x" + n) : name, NAME_X, y, skin.ink(), false);
            }
            if (!on)
                continue;
            fit(g, choice(role).<Component>map(l -> Component.translatable("material." + l.id().getNamespace() + "." + l.id().getPath()))
                    .orElse(Component.translatable("gui.feedback.blueprint_table.nothing_fits")), MAT_X, y, MAT_W, skin.ink());
            ToolTableShaping.Option shape = shapeOf(role);
            if (shape != null && shapes(role).size() > 1) {
                shapeBoxes.put(role, new int[]{SHAPE_X - 14, ROW_Y + i * ROW_H, SHAPE_W + 28, ROW_H});
            }
        }
        if (edged())
            g.drawString(font, Component.translatable("gui.feedback.blueprint_table.edge"), NAME_X, ROW_Y + rows.size() * ROW_H + 3, skin.ink(), false);
        figureBoxes.clear();
        Optional<ModularDesign> design = design();
        if (!design.equals(Optional.ofNullable(baked))) {
            baked = design.orElse(null);
            figures = design.flatMap(d -> ToolDesigns.predict(d, ModularTypes.known(), ClientMaterials.get()));
            modifiers = design.isPresent() && figures.isPresent() ? ModularModifiers.of(design.get(), current(), figures.get()) : List.of();
            if (fresh && figures.isPresent()) {
                reference = figures;
                fresh = false;
            }
            changes = figures.isPresent() && reference.isPresent() ? FigureCompare.lines(reference.get(), figures.get()) : List.of();
        }
        changeBox = null;
        modifierBox = null;
        int top = ROW_Y + (rows.size() + (edged() ? 1 : 0)) * ROW_H + 6;
        g.drawString(font, Component.translatable("gui.feedback.blueprint_table.as_drawn"), 8, top, skin.ink(), false);
        if (figures.isEmpty()) {
            g.drawString(font, Component.translatable("message.feedback.blueprint_table.cannot_print"), 8, top + 11, BAD, false);
            return;
        }
        int y = top + 11;
        var line = ModularModifiers.line(modifiers);
        if (line != null) {
            g.drawString(font, line, 8, y, skin.ink(), false);
            modifierBox = new int[]{8, y, font.width(line)};
            y += 11;
        }
        List<Component> lines = ModularBlueprintItem.figures(figures.get(), true);
        int half = (lines.size() + 1) / 2;
        for (int i = 0; i < lines.size(); i++) {
            int x = i < half ? 8 : imageWidth / 2 + 4, ly = y + (i % half) * 9;
            g.drawString(font, lines.get(i), x, ly, skin.ink(), false);
            figureBoxes.add(new int[]{x, ly, font.width(lines.get(i))});
        }
        if (!changes.isEmpty()) {
            Component hint = Component.translatable("gui.feedback.blueprint_table.changed");
            changeBox = new int[]{8, y + 2 + half * 9, font.width(hint)};
            g.drawString(font, hint, changeBox[0], changeBox[1], skin.ink(), false);
        }
    }

    /** Hovering a shape says what it does, a modifier says what it means, a figure's line says what it is for the item. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        int mx = mouseX - leftPos, my = mouseY - topPos;
        for (Map.Entry<String, int[]> e : shapeBoxes.entrySet()) {
            int[] b = e.getValue();
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                ToolTableShaping.Option o = shapeOf(e.getKey());
                List<Component> tip = new ArrayList<>();
                tip.add(o.label());
                ModularModifiers.forPart(o).forEach(m -> tip.add(m.line()));
                g.renderTooltip(font, tip.stream().flatMap(c -> font.split(c, 200).stream()).toList(), mouseX, mouseY);
                return;
            }
        }
        if (modifierBox != null && mx >= modifierBox[0] && mx < modifierBox[0] + modifierBox[2] && my >= modifierBox[1] && my < modifierBox[1] + 9) {
            g.renderTooltip(font, modifiers.stream().flatMap(m -> font.split(m.line(), 200).stream()).toList(), mouseX, mouseY);
            return;
        }
        if (figures.isEmpty())
            return;
        List<ModularFigures.Fig> present = ModularBlueprintItem.present(figures.get());
        if (changeBox != null && mx >= changeBox[0] && mx < changeBox[0] + changeBox[2] && my >= changeBox[1] && my < changeBox[1] + 9) {
            g.renderTooltip(font, changes, java.util.Optional.empty(), mouseX, mouseY);
            return;
        }
        for (int i = 0; i < figureBoxes.size() && i < present.size(); i++) {
            int[] b = figureBoxes.get(i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + 9) {
                List<net.minecraft.util.FormattedCharSequence> tip = new ArrayList<>();
                for (Component c : ModularBlueprintItem.explain(present.get(i)))
                    tip.addAll(font.split(c, 180));
                g.renderTooltip(font, tip, mouseX, mouseY);
                return;
            }
        }
    }
}
