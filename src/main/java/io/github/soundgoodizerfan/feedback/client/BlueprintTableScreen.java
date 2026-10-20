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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.BlueprintTableMenu;
import io.github.soundgoodizerfan.feedback.modular.Edge;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
import io.github.soundgoodizerfan.feedback.modular.ModularBlueprintItem;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
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
 * The Blueprint Table: pick a type, its pattern (which parts it has, and how long its handle is),
 * a material for each role and how the edge is ground, and read what that design bakes to. The
 * figures are {@code ModularBake} run on the drawing, so they are the design's, not any item's
 * (spec 6: the blueprint predicts, instruments measure). Roles are read off the type's file, so a
 * role added to a type appears here with no change. Drawn with {@link GuiStyle} and flat fills;
 * the colours are the constants below, so a reskin is this file's top and {@code GuiStyle}.
 */
public class BlueprintTableScreen extends SkinnedScreen<BlueprintTableMenu> {

    private static final int SHEET_X = 8, SHEET_Y = 20, SHEET = 96, CELL = 20, ROWS_X = 108, ROW_H = 16,
            MINUS_X = 150, PLUS_X = 246;
    private static final int PAPER = 0xFF1C5FA8, LINE = 0xFFFFFFFF, FAINT = 0x33FFFFFF, BAD = 0xFFAA0000;

    private final List<ResourceLocation> types = ModularTypes.known().entrySet().stream()
            .filter(e -> e.getValue().family() != ModularType.Family.GUN).map(java.util.Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toCollection(ArrayList::new)); // a gun is drawn at the Gun Stand
    private int type;
    private int pattern;
    private Edge edge = Edge.STANDARD;
    /** Per role letter, which of its choices is drawn. */
    private final Map<String, Integer> picked = new HashMap<>();
    private Map<String, List<Material.Located>> choices = Map.of();
    /** The paper last seen, so only a blueprint newly put in is opened. */
    private ItemStack lastPaper = ItemStack.EMPTY;
    private ModularDesign baked;
    private Optional<ModularFigures> figures = Optional.empty();

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.BLUEPRINT);
        imageWidth = 264;
        imageHeight = 256;
        inventoryLabelY = 10_000; // only the hotbar is shown
        titleLabelY = 10_000; // the type's name is the title
        selectType(0);
    }

    private ModularType current() {
        return types.isEmpty() ? null : ModularTypes.known().get(types.get(type));
    }

    /** The roles the pattern has, in the type's order. */
    private List<String> roles() {
        ModularType t = current();
        if (t == null)
            return List.of();
        Map<String, Integer> counts = t.counts(pattern);
        return t.order().stream().filter(counts::containsKey).toList();
    }

    private void selectType(int index) {
        type = types.isEmpty() ? 0 : Math.floorMod(index, types.size());
        pattern = 0;
        picked.clear();
        ModularType t = current();
        if (t == null)
            return;
        Map<ResourceLocation, Material> materials = ClientMaterials.get();
        choices = new HashMap<>();
        t.roles().forEach((letter, role) -> {
            List<Material.Located> c = ModularBlueprintItem.choices(role, materials);
            choices.put(letter, c);
            ResourceLocation fallback = t.defaults().get(letter);
            for (int i = 0; i < c.size(); i++)
                if (c.get(i).id().equals(fallback))
                    picked.put(letter, i);
        });
    }

    /** Open a blueprint: its type, the pattern its parts make, and each part's material where it is a choice. */
    private void open(ModularDesign design) {
        int index = types.indexOf(design.type());
        ModularType t = index < 0 ? null : ModularTypes.known().get(design.type());
        if (t == null)
            return;
        selectType(index);
        edge = design.edge();
        Map<String, Integer> drawn = new HashMap<>();
        design.parts().forEach(p -> drawn.put(p.role(), p.count()));
        for (int i = 0; i < t.patterns().size(); i++)
            if (t.counts(i).equals(drawn))
                pattern = i;
        design.parts().forEach(p -> {
            List<Material.Located> c = choices.getOrDefault(p.role(), List.of());
            for (int i = 0; i < c.size(); i++)
                if (c.get(i).id().equals(p.material()))
                    picked.put(p.role(), i);
        });
    }

    private Optional<Material.Located> choice(String role) {
        List<Material.Located> c = choices.getOrDefault(role, List.of());
        return c.isEmpty() ? Optional.empty() : Optional.of(c.get(Math.floorMod(picked.getOrDefault(role, 0), c.size())));
    }

    /** What is drawn, as a design; empty while a role has no material that fits it. */
    private Optional<ModularDesign> design() {
        ModularType t = current();
        if (t == null)
            return Optional.empty();
        Map<String, Integer> counts = t.counts(pattern);
        List<ModularDesign.Part> parts = new ArrayList<>();
        for (String role : roles()) {
            Optional<Material.Located> c = choice(role);
            if (c.isEmpty())
                return Optional.empty();
            parts.add(new ModularDesign.Part(role, c.get().id(), c.get().form(), counts.get(role)));
        }
        return Optional.of(new ModularDesign(types.get(type), parts, edge));
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> change(() -> selectType(type - 1)), leftPos + 8, topPos + 4, 14, 12));
        addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> change(() -> selectType(type + 1)), leftPos + imageWidth - 22, topPos + 4, 14, 12));
        ModularType t = current();
        if (t != null && t.patterns().size() > 1) {
            int y = topPos + SHEET_Y + SHEET + 2;
            addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> change(() -> pattern = Math.floorMod(pattern - 1, t.patterns().size())), leftPos + SHEET_X, y, 14, 12));
            addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> change(() -> pattern = Math.floorMod(pattern + 1, t.patterns().size())), leftPos + SHEET_X + SHEET - 14, y, 14, 12));
        }
        List<String> roles = roles();
        for (int i = 0; i <= roles.size(); i++) {
            int y = topPos + SHEET_Y + i * ROW_H;
            String role = i < roles.size() ? roles.get(i) : null; // the last row is the edge
            addRenderableWidget(GuiStyle.button(skin, Component.literal("<"), b -> step(role, -1), leftPos + MINUS_X, y, 14, 14));
            addRenderableWidget(GuiStyle.button(skin, Component.literal(">"), b -> step(role, 1), leftPos + PLUS_X, y, 14, 14));
        }
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.gun_stand.print"),
                b -> design().ifPresent(d -> PacketDistributor.sendToServer(new ModularBlueprintPayload(d))),
                leftPos + BlueprintTableMenu.PAPER_X + 20, topPos + BlueprintTableMenu.PAPER_Y, 42, 16));
    }

    private void change(Runnable edit) {
        edit.run();
        rebuildWidgets();
    }

    /** A role's material, or the edge for {@code null}, one along. */
    private void step(String role, int by) {
        if (role == null)
            edge = edge.step(by);
        else
            picked.merge(role, by, Integer::sum);
    }

    /** A blueprint put in the slot opens on the sheet. */
    @Override
    protected void containerTick() {
        super.containerTick();
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
        sheet(g);
    }

    /** The pattern on blueprint paper, each cell its role's material colour. */
    private void sheet(GuiGraphics g) {
        int x0 = leftPos + SHEET_X, y0 = topPos + SHEET_Y;
        g.fill(x0, y0, x0 + SHEET, y0 + SHEET, PAPER);
        for (int d = CELL / 2; d < SHEET; d += CELL / 2) {
            g.vLine(x0 + d, y0, y0 + SHEET, FAINT);
            g.hLine(x0, x0 + SHEET - 1, y0 + d, FAINT);
        }
        ModularType t = current();
        if (t == null)
            return;
        List<String> rows = t.patterns().get(pattern);
        int w = rows.stream().mapToInt(String::length).max().orElse(0);
        int ox = x0 + (SHEET - w * CELL) / 2, oy = y0 + (SHEET - rows.size() * CELL) / 2;
        for (int r = 0; r < rows.size(); r++)
            for (int c = 0; c < rows.get(r).length(); c++) {
                String letter = String.valueOf(rows.get(r).charAt(c));
                if (letter.equals(" "))
                    continue;
                int x = ox + c * CELL, y = oy + r * CELL;
                int fill = choice(letter).map(l -> 0xFF000000 | MaterialColors.of(l.id())).orElse(BAD);
                g.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, fill);
                g.renderOutline(x, y, CELL, CELL, LINE);
            }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ResourceLocation id = types.isEmpty() ? null : types.get(type);
        if (id != null)
            g.drawCenteredString(font, Component.translatable("item." + id.getNamespace() + "." + id.getPath()), imageWidth / 2, 6, 0xFFFFFF);
        ModularType t = current();
        if (t != null && t.patterns().size() > 1)
            g.drawCenteredString(font, Component.translatable("gui.feedback.blueprint_table.pattern", pattern + 1, t.patterns().size()),
                    SHEET_X + SHEET / 2, SHEET_Y + SHEET + 4, 0xFFFFFF);
        List<String> roles = roles();
        for (int i = 0; i <= roles.size(); i++) {
            int y = SHEET_Y + i * ROW_H + 3;
            Component name, value;
            if (i < roles.size()) {
                name = Component.translatable("modular.feedback.role." + t.roles().get(roles.get(i)).name());
                value = choice(roles.get(i)).<Component>map(l -> Component.translatable(
                        "material." + l.id().getNamespace() + "." + l.id().getPath()))
                        .orElse(Component.translatable("gui.feedback.blueprint_table.nothing_fits"));
            } else {
                name = Component.translatable("gui.feedback.blueprint_table.edge");
                value = Component.translatable("tooltip.feedback.modular.edge." + edge.getSerializedName());
            }
            g.drawString(font, name, ROWS_X, y, skin.ink(), false);
            g.drawCenteredString(font, value, (MINUS_X + 14 + PLUS_X) / 2, y, 0xFFFFFF);
        }
        Optional<ModularDesign> design = design();
        if (!design.equals(Optional.ofNullable(baked))) {
            baked = design.orElse(null);
            figures = design.flatMap(d -> ModularBlueprintItem.predict(d, ModularTypes.known(), ClientMaterials.get()));
        }
        // Under the sheet, or under the last row if a type has more roles than the sheet is tall.
        int top = Math.max(SHEET_Y + SHEET + 18, SHEET_Y + (roles.size() + 1) * ROW_H + 4);
        g.drawString(font, Component.translatable("gui.feedback.blueprint_table.as_drawn"), 8, top, skin.ink(), false);
        if (figures.isEmpty()) {
            g.drawString(font, Component.translatable("message.feedback.blueprint_table.cannot_print"), 8, top + 11, BAD, false);
            return;
        }
        List<Component> lines = ModularBlueprintItem.figures(figures.get());
        int half = (lines.size() + 1) / 2;
        for (int i = 0; i < lines.size(); i++)
            g.drawString(font, lines.get(i), i < half ? 8 : imageWidth / 2 + 4, top + 11 + (i % half) * 9, skin.ink(), false);
    }
}
