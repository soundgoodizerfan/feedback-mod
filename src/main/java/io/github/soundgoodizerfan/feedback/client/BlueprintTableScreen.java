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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import io.github.soundgoodizerfan.feedback.modular.BlueprintTableMenu;
import io.github.soundgoodizerfan.feedback.modular.Edge;
import io.github.soundgoodizerfan.feedback.modular.MaterialColors;
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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Blueprint Table: draw a tool on paper and read what it bakes to. Everything it offers comes
 * from {@link ToolDesigns}, the same model the Tool Table makes things from, so a drawing is exactly
 * what the Tool Table would accept and the figures are the ones of the item it would make (spec 6,
 * 4.2: the blueprint predicts, instruments measure). Heat and flaws are not drawn: a flaw is what
 * forging adds.
 *
 * <h2>The sheet</h2>
 * Top: the type (sword, axe, gun...). There is no form to pick: which parts the build has is just which
 * options are on -- the optional parts and the parts some forms of the type have and others do not (a gun's
 * slide or bolt, cylinder or tube), found by {@link #now}. Then a list with a bar per part drawn; the selected bar's material, shape (and
 * edge) are set in the detail panel under the list, so the list itself stays short and readable. The last
 * bar is <em>+ add part</em>: it adds an optional part as a bar with its own arrows, to swap it for any other
 * optional part. Stats sit under the panel.
 */
public class BlueprintTableScreen extends SkinnedScreen<BlueprintTableMenu> {

    private static final int ROW_Y = 22, ROW_H = 18, LIST_X = 8, NAME_X = 14;
    /** Bars on the page at once; a longer list scrolls with the wheel. */
    private static final int VISIBLE = 5;
    private static final int DET_Y = ROW_Y + VISIBLE * ROW_H + 4, DET_H = 56, DET_ROW = 17;
    private static final int CTL_X = 84, CTL_W = 190;
    /** The search button beside a stepper. */
    private static final int SEARCH_W = 16;
    private static final int STATS_Y = DET_Y + DET_H + 6;
    private static final int BAD = 0xFFAA0000;
    /** The list item that adds a part. */
    private static final String ADD = "+";

    /** A box that explains itself when hovered, in screen coordinates. */
    private record Tip(int x, int y, int w, int h, Supplier<List<Component>> lines, int width) {
    }

    private final List<ResourceLocation> types = ToolDesigns.drawable(ModularTypes.known());
    private int type;
    private int form;
    private final Set<String> extras = new LinkedHashSet<>();
    /** The size of each ranged part (a charge's filler), beside the pattern: a part with a count is one part. */
    private final Map<String, Integer> sizes = new HashMap<>();
    /** The first list item on the page. */
    private int rowScroll;
    /** The window that opens over the sheet to pick an optional part to add. */
    private final SkinPopup<String> addPopup = new SkinPopup<>(skin, Component.translatable("gui.feedback.blueprint_table.add_part_title"));
    /** The searchable windows that pick a type or a material by name instead of stepping to it. */
    private final SkinPopup<Integer> typePopup = new SkinPopup<>(skin, Component.translatable("gui.feedback.blueprint_table.pick_type"));
    private final SkinPopup<Integer> materialPopup = new SkinPopup<>(skin, Component.translatable("gui.feedback.blueprint_table.pick_material"));
    /** The part whose detail is shown; null picks the first. */
    private String selected;
    private Edge edge = Edge.STANDARD;
    /** Per role letter, which of its shapes and which of its materials is drawn. */
    private final Map<String, Integer> shape = new HashMap<>();
    private final Map<String, Integer> material = new HashMap<>();
    /** What the sample slot held when last read, so a sample sets its material once and the stepper can then change it. */
    private ItemStack seen = ItemStack.EMPTY;
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
    private final List<Tip> tips = new ArrayList<>();
    /** False until the first edit after a type is picked: the form stage. */
    private boolean touched;
    private int lastStage;
    /** Width of the type stepper; the search button, then the stage strip, follow it. */
    private static final int TYPE_W = 130;

    public BlueprintTableScreen(BlueprintTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.BLUEPRINT);
        imageWidth = 300;
        imageHeight = BlueprintTableMenu.H;
        inventoryLabelY = 10_000; // only the hotbar is shown
        titleLabelY = 10_000; // the type's name is the title
        selectType(0);
        touched = false;
    }

    /** Form until the first edit, result once the drawing prices out, draw while paper waits in the slot (stock is the unfitting in-between). */
    private int stage() {
        if (!touched)
            return 0;
        if (figures.isEmpty())
            return 1;
        return menu.getSlot(0).hasItem() ? 3 : 2;
    }

    private ModularType current() {
        return types.isEmpty() ? null : ModularTypes.known().get(types.get(type));
    }

    private boolean edged() {
        ModularType t = current();
        return t != null && t.edged();
    }

    // --- what is on the sheet ---------------------------------------------------------------------

    private Map<String, Integer> counts() {
        return ToolDesigns.counts(current(), form, extras, sizes);
    }

    /** The roles drawn, in the type's order. */
    private List<String> rows() {
        ModularType t = current();
        if (t == null)
            return List.of();
        Map<String, Integer> counts = counts();
        return t.order().stream().filter(counts::containsKey).toList();
    }

    /** The list: every part drawn, then the add bar while there are optional parts left. */
    private List<String> items() {
        List<String> out = new ArrayList<>(rows());
        if (!offered().isEmpty())
            out.add(ADD);
        return out;
    }

    /** The items on the page. */
    private List<String> visibleItems() {
        List<String> all = items();
        rowScroll = Math.max(0, Math.min(rowScroll, all.size() - VISIBLE));
        return all.subList(rowScroll, Math.min(all.size(), rowScroll + VISIBLE));
    }

    /** The selected part: the one picked if it is still drawn, else the first. */
    private String sel() {
        List<String> rows = rows();
        if (selected == null || !rows.contains(selected))
            selected = rows.isEmpty() ? null : rows.get(0);
        return selected;
    }

    private Component roleName(String role) {
        return Component.translatable("modular.feedback.role." + current().roles().get(role).name());
    }

    /** What a part is for: its own line in the lang file, if it has one. */
    private List<Component> roleTip(String role) {
        String name = current().roles().get(role).name();
        List<Component> out = new ArrayList<>();
        out.add(roleName(role));
        if (I18n.exists("modular.feedback.role." + name + ".tip"))
            out.add(Component.translatable("modular.feedback.role." + name + ".tip"));
        return out;
    }

    // --- options: the form is never chosen, it follows from the parts added -----------------------------
    // An option is a key: an optional part's role letter, or "role:count" for an entry that is in some
    // forms of the type and not others (a gun's slide, bolt or cylinder; a handle of two). The sheet is
    // the options on; adding, removing or swapping one is allowed when some form has exactly that set.

    private static boolean pair(String key) {
        return key.indexOf(':') >= 0;
    }

    private static String roleOf(String key) {
        return pair(key) ? key.substring(0, key.indexOf(':')) : key;
    }

    private ModularType derivedFor;
    private List<Map<String, Integer>> forms = List.of();
    private List<Set<String>> varied = List.of();
    private List<String> keys = List.of();

    /** The type's forms and what varies in each, worked out once per type: the sheet asks every frame, and a charge has seventy-two forms. */
    private void derive() {
        ModularType t = current();
        if (t == derivedFor)
            return;
        derivedFor = t;
        forms = t == null ? List.of() : t.baseForms();
        varied = new ArrayList<>();
        Set<String> all = new LinkedHashSet<>();
        for (Map<String, Integer> f : forms) {
            Set<String> out = new LinkedHashSet<>();
            f.forEach((r, n) -> {
                if (!t.ranged(r) && !forms.stream().allMatch(o -> Objects.equals(o.get(r), n)))
                    out.add(r + ":" + n);
            });
            varied.add(out);
            all.addAll(out);
        }
        if (t != null)
            all.addAll(t.optionals());
        keys = List.copyOf(all);
    }

    private Set<String> varying(int i) {
        derive();
        return varied.get(i);
    }

    /** Every option the sheet has on. */
    private Set<String> now() {
        derive();
        Set<String> out = new LinkedHashSet<>(varying(Math.floorMod(form, forms.size())));
        out.addAll(extras);
        return out;
    }

    private int formFor(Set<String> target) {
        Set<String> want = new LinkedHashSet<>();
        target.stream().filter(BlueprintTableScreen::pair).forEach(want::add);
        derive();
        for (int i = 0; i < forms.size(); i++)
            if (varying(i).equals(want))
                return i;
        return -1;
    }

    private boolean feasible(Set<String> target) {
        return formFor(target) >= 0;
    }

    /** Turn the sheet to exactly {@code target}, if some form has it. */
    private void set(Set<String> target) {
        int f = formFor(target);
        if (f < 0)
            return;
        form = f;
        extras.clear();
        target.stream().filter(k -> !pair(k)).forEach(extras::add);
    }

    private List<String> allKeys() {
        derive();
        return keys;
    }

    /** The parts that can be added now. */
    private List<String> offered() {
        if (current() == null)
            return List.of();
        Set<String> cur = now();
        return allKeys().stream().filter(k -> !cur.contains(k)).filter(k -> {
            Set<String> s = new LinkedHashSet<>(cur);
            s.add(k);
            return feasible(s);
        }).toList();
    }

    /**
     * {@code key} and what it can be swapped for, keeping the sheet one some form has, in the one fixed order of
     * {@link #allKeys}: the arrows step round this ring, so the same list is walked both ways.
     */
    private List<String> swaps(String key) {
        Set<String> cur = now();
        List<String> out = new ArrayList<>();
        for (String k : allKeys()) {
            if (k.equals(key)) {
                out.add(k);
                continue;
            }
            if (cur.contains(k) || pair(k) != pair(key))
                continue;
            Set<String> s = new LinkedHashSet<>(cur);
            s.remove(key);
            s.add(k);
            if (feasible(s))
                out.add(k);
        }
        return out;
    }

    private boolean removable(String key) {
        Set<String> s = now();
        s.remove(key);
        return feasible(s);
    }

    /** The option a drawn role is, or null for a part every build of the type has. */
    private String keyOf(String role) {
        Set<String> cur = now();
        if (cur.contains(role))
            return role;
        String k = role + ":" + counts().getOrDefault(role, 1);
        return cur.contains(k) ? k : null;
    }

    /** What an option is called: the part, and how many where it is raw stock. */
    private String keyLabel(String key) {
        String role = roleOf(key);
        int n = pair(key) ? Integer.parseInt(key.substring(key.indexOf(':') + 1)) : 1;
        String name = roleName(role).getString();
        return n > 1 && !ToolDesigns.shaped(current().roles().get(role)) ? name + " x" + n : name;
    }

    private void swap(String key, String next) {
        Set<String> s = now();
        s.remove(key);
        s.add(next);
        set(s);
        selected = roleOf(next);
    }

    private boolean drawn(String role) {
        return counts().containsKey(role);
    }

    private void selectType(int index) {
        type = types.isEmpty() ? 0 : Math.floorMod(index, types.size());
        fresh = true;
        rowScroll = 0;
        selected = null;
        form = 0;
        extras.clear();
        sizes.clear();
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
        sizes.putAll(opened.get().sizes());
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

    private Component typeName(int i) {
        ResourceLocation id = types.get(Math.floorMod(i, types.size()));
        return Component.translatable("item." + id.getNamespace() + "." + id.getPath());
    }

    private static Component materialLabel(Material.Located l) {
        return Component.translatable("material." + l.id().getNamespace() + "." + l.id().getPath());
    }

    private String materialName(String role) {
        return choice(role).<Component>map(l -> Component.translatable("material." + l.id().getNamespace() + "." + l.id().getPath()))
                .orElse(Component.translatable("gui.feedback.blueprint_table.nothing_fits")).getString();
    }

    /** What is drawn, as a design; empty while a role has no material that fits it. */
    private Optional<ModularDesign> design() {
        ModularType t = current();
        if (t == null)
            return Optional.empty();
        Map<String, Integer> counts = counts();
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

    // --- widgets ----------------------------------------------------------------------------------

    private void tip(int x, int y, int w, int h, int width, Supplier<List<Component>> lines) {
        tips.add(new Tip(x, y, w, h, lines, width));
    }

    /** The material and shape in words, for the right of a part's bar. */
    private String summary(String role) {
        ToolTableShaping.Option o = shapeOf(role);
        return materialName(role) + (o != null && shapes(role).size() > 1 && !o.variant().name().isEmpty() ? ", " + o.label().getString() : "");
    }

    @Override
    protected void init() {
        super.init();
        tips.clear();
        addRenderableWidget(GuiStyle.stepper(skin, leftPos + 8, topPos + 4, TYPE_W, 12, "<", ">",
                () -> types.isEmpty() ? "" : typeName(type).getString(),
                dir -> change(() -> selectType(type + dir)), false, null));
        addRenderableWidget(GuiStyle.button(skin, Component.literal("..."), b -> typePopup.open(
                IntStream.range(0, types.size()).mapToObj(i -> new SkinPopup.Entry<>(i, typeName(i), List.<Component>of())).toList(),
                i -> change(() -> selectType(i))), leftPos + 8 + TYPE_W + 2, topPos + 4, SEARCH_W, 12));
        // Stage strip, display only like the Tool Table's.
        addRenderableOnly(GuiStyle.tabs(skin, leftPos + 8 + TYPE_W + 2 + SEARCH_W + 4, topPos + 4, imageWidth - 16 - TYPE_W - 2 - SEARCH_W - 4, 12,
                List.of("form", "stock", "result", "draw").stream().<Component>map(k -> Component.translatable("gui.feedback.blueprint_table.stage." + k)).toList(),
                this::stage, i -> { }));
        tip(leftPos + 8 + TYPE_W + 2, topPos + 4, SEARCH_W, 12, 150, () -> List.of(Component.translatable("gui.feedback.blueprint_table.search")));
        ModularType t = current();
        if (t == null)
            return;
        List<String> items = visibleItems();
        for (int i = 0; i < items.size(); i++) {
            String role = items.get(i);
            int y = topPos + ROW_Y + i * ROW_H;
            if (role.equals(ADD))
                continue;
            String key = keyOf(role);
            boolean rem = key != null && removable(key);
            List<String> sw = key == null ? List.of() : swaps(key);
            int x = leftPos + LIST_X + 3;
            if (rem) {
                addRenderableWidget(GuiStyle.button(skin, Component.literal("x"), b -> change(() -> {
                    Set<String> rest = now();
                    rest.remove(key);
                    set(rest);
                }), x, y + 2, 12, 14));
                tip(x, y + 2, 12, 14, 150, () -> List.of(Component.translatable("gui.feedback.blueprint_table.remove")));
                x += 15;
            }
            if (t.ranged(role)) {
                List<Integer> range = t.ranges().get(role);
                int lo = Math.max(1, range.get(0)), hi = range.get(1);
                addRenderableWidget(GuiStyle.stepper(skin, x, y + 2, 130, 14, "<", ">",
                        () -> roleName(role).getString() + " x" + counts().get(role),
                        dir -> change(() -> sizes.put(role, lo + Math.floorMod(counts().get(role) - lo + dir, hi - lo + 1))), false, null));
                tip(x, y + 2, 130, 14, 190, () -> roleTip(role));
            } else if (sw.size() > 1) {
                addRenderableWidget(GuiStyle.stepper(skin, x, y + 2, 110, 14, "<", ">", () -> keyLabel(key),
                        dir -> change(() -> swap(key, sw.get(Math.floorMod(sw.indexOf(key) + dir, sw.size())))), false, null));
                tip(x, y + 2, 110, 14, 190, () -> roleTip(role));
            } else
                tip(x, y, 100, ROW_H, 190, () -> roleTip(role));
        }
        String role = sel();
        if (role != null) {
            menu.shown(List.of(true));
            int y = topPos + DET_Y + 4;
            List<Material.Located> mats = materials(role);
            addRenderableWidget(GuiStyle.stepper(skin, leftPos + CTL_X, y + 1, CTL_W - SEARCH_W - 2, 14, "<", ">", () -> materialName(role),
                    dir -> change(() -> {
                        if (!mats.isEmpty())
                            material.put(role, Math.floorMod(material.getOrDefault(role, 0) + dir, mats.size()));
                    }), false, null));
            addRenderableWidget(GuiStyle.button(skin, Component.literal("..."), b -> materialPopup.open(
                    IntStream.range(0, mats.size()).mapToObj(i -> new SkinPopup.Entry<>(i, materialLabel(mats.get(i)), List.<Component>of())).toList(),
                    i -> change(() -> material.put(role, i))), leftPos + CTL_X + CTL_W - SEARCH_W, y + 1, SEARCH_W, 14));
            tip(leftPos + CTL_X + CTL_W - SEARCH_W, y + 1, SEARCH_W, 14, 150, () -> List.of(Component.translatable("gui.feedback.blueprint_table.search")));
            tip(leftPos + BlueprintTableMenu.SAMPLE_X - 1, topPos + BlueprintTableMenu.SAMPLE_Y - 1, 18, 18, 170, () -> menu.sample(0).isEmpty()
                    ? List.of(Component.translatable("gui.feedback.blueprint_table.sample")) : List.of());
            y += DET_ROW;
            List<ToolTableShaping.Option> shapes = shapes(role);
            if (shapes.size() > 1) {
                addRenderableWidget(GuiStyle.stepper(skin, leftPos + CTL_X, y + 1, CTL_W, 14, "<", ">",
                        () -> shapeOf(role) == null ? "" : shapeOf(role).label().getString(),
                        dir -> change(() -> shape.put(role, Math.floorMod(shape.getOrDefault(role, 0) + dir, shapes.size()))), false, null));
                tip(leftPos + CTL_X, y + 1, CTL_W, 14, 200, () -> {
                    List<Component> out = new ArrayList<>();
                    out.add(shapeOf(role).label());
                    ModularModifiers.forPart(shapeOf(role)).forEach(m -> out.add(m.line()));
                    return out;
                });
                y += DET_ROW;
            }
            if (edged() && role.equals(t.wearRole()))
                addRenderableWidget(GuiStyle.stepper(skin, leftPos + CTL_X, y + 1, CTL_W, 14, "<", ">",
                        () -> Component.translatable("tooltip.feedback.modular.edge." + edge.getSerializedName()).getString(),
                        dir -> change(() -> edge = edge.step(dir)), false, null));
        } else
            menu.shown(List.of());
        printButton();
    }

    private void printButton() {
        addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.blueprint_table.print"),
                b -> {
                    if (design().isPresent() && figures.isPresent() && menu.getSlot(0).hasItem()) {
                        skin.cue(GuiSkin.Cue.FINISH); // the stamp
                        PacketDistributor.sendToServer(new ModularBlueprintPayload(design().get()));
                    } else
                        skin.cue(GuiSkin.Cue.REJECT);
                },
                leftPos + BlueprintTableMenu.PAPER_X + 20, topPos + BlueprintTableMenu.PAPER_Y, 42, 16));
        tip(leftPos + BlueprintTableMenu.PAPER_X, topPos + BlueprintTableMenu.PAPER_Y, 16, 16, 190, () -> menu.getSlot(0).hasItem()
                ? List.of() : List.of(Component.translatable("gui.feedback.blueprint_table.paper")));
    }

    /** Make an edit and rebuild the sheet; if it changed which part is selected or on the page, the sample meant another part, so it goes back to the player. */
    private void change(Runnable edit) {
        List<String> before = new ArrayList<>(visibleItems());
        before.add(String.valueOf(sel()));
        int type0 = type;
        edit.run();
        touched = type == type0;
        List<String> after = new ArrayList<>(visibleItems());
        after.add(String.valueOf(sel()));
        if (!before.equals(after) && net.minecraft.client.Minecraft.getInstance().gameMode != null)
            net.minecraft.client.Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, BlueprintTableMenu.BTN_CLEAR);
        rebuildWidgets();
    }

    private boolean overList(double mouseX, double mouseY) {
        return mouseX >= leftPos + LIST_X && mouseX < leftPos + imageWidth - 12 && mouseY >= topPos + ROW_Y && mouseY < topPos + ROW_Y + VISIBLE * ROW_H;
    }

    /** A bar clicked selects its part; the add bar adds one. */
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int s = stage();
        if (s != lastStage) {
            skin.cue(GuiSkin.Cue.STAGE);
            lastStage = s;
        }
        super.render(g, mouseX, mouseY, partialTick);
        addPopup.render(g, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        typePopup.render(g, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        materialPopup.render(g, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        return addPopup.keyPressed(key, scan, modifiers) || typePopup.keyPressed(key, scan, modifiers)
                || materialPopup.keyPressed(key, scan, modifiers) || super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        return addPopup.charTyped(c, modifiers) || typePopup.charTyped(c, modifiers)
                || materialPopup.charTyped(c, modifiers) || super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (addPopup.mouseClicked(mouseX, mouseY, button) || typePopup.mouseClicked(mouseX, mouseY, button)
                || materialPopup.mouseClicked(mouseX, mouseY, button))
            return true;
        // The container screen answers every click it is given, so the bars are tried first, but never under a widget.
        boolean onWidget = children().stream().anyMatch(c -> c.isMouseOver(mouseX, mouseY));
        if (settled() && button == 0 && !onWidget && overList(mouseX, mouseY)) {
            List<String> items = visibleItems();
            int i = (int) (mouseY - topPos - ROW_Y) / ROW_H;
            if (i < items.size()) {
                GuiSkin.play(skin.click(), 1f);
                String role = items.get(i);
                if (role.equals(ADD)) {
                    addPopup.open(offered().stream().map(k -> new SkinPopup.Entry<>(k, Component.literal(keyLabel(k)), roleTip(roleOf(k)))).toList(),
                            k -> change(() -> {
                                Set<String> on = now();
                                on.add(k);
                                set(on);
                                selected = roleOf(k);
                            }));
                } else
                    change(() -> selected = role);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (addPopup.mouseScrolled(scrollY) || typePopup.mouseScrolled(scrollY) || materialPopup.mouseScrolled(scrollY))
            return true;
        int total = items().size();
        if (total > VISIBLE && overList(mouseX, mouseY)) {
            int target = Math.max(0, Math.min(total - VISIBLE, rowScroll - (int) Math.signum(scrollY)));
            if (target != rowScroll)
                change(() -> rowScroll = target);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // --- samples ----------------------------------------------------------------------------------

    /** A sample put in the slot sets the selected part's material, once: the stepper can change it after. */
    private void readSamples() {
        ItemStack sample = menu.sample(0);
        if (ItemStack.isSameItemSameComponents(sample, seen))
            return;
        seen = sample.copy();
        String role = sel();
        if (sample.isEmpty() || role == null)
            return;
        Optional<Material.Located> named = Material.find(ClientMaterials.get(), sample);
        if (named.isEmpty()) {
            skin.cue(GuiSkin.Cue.REJECT);
            return;
        }
        List<Material.Located> c = materials(role);
        for (int k = 0; k < c.size(); k++)
            if (c.get(k).id().equals(named.get().id())) {
                skin.cue(GuiSkin.Cue.SEAT, null, 1f); // pencil
                if (k != material.getOrDefault(role, 0)) {
                    material.put(role, k);
                    rebuildWidgets();
                }
                break;
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

    // --- drawing ----------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        skin.slot(g, leftPos + BlueprintTableMenu.PAPER_X, topPos + BlueprintTableMenu.PAPER_Y);
        for (int i = 0; i < 9; i++)
            skin.slot(g, leftPos + BlueprintTableMenu.HOTBAR_X + i * 18, topPos + BlueprintTableMenu.HOTBAR_Y);
        skin.inset(g, leftPos + LIST_X - 2, topPos + ROW_Y - 2, imageWidth - 12, VISIBLE * ROW_H + 4);
        List<String> items = visibleItems();
        String sel = sel();
        for (int i = 0; i < items.size(); i++) {
            String role = items.get(i);
            int y = topPos + ROW_Y + i * ROW_H;
            int w = imageWidth - 20;
            skin.plaque(g, leftPos + LIST_X, y + 1, w, ROW_H - 2);
            boolean hovered = overList(mouseX, mouseY) && (mouseY - topPos - ROW_Y) / ROW_H == i;
            skin.row(g, leftPos + LIST_X, y + 1, w, ROW_H - 2, role.equals(sel), hovered);
            if (!role.equals(ADD)) {
                // The part's material colour, as a stripe on its bar.
                int rgb = choice(role).map(l -> MaterialColors.of(l.id())).orElse(0x808080);
                g.fill(leftPos + LIST_X + 1, y + 2, leftPos + LIST_X + 3, y + ROW_H - 2, 0xFF000000 | rgb);
            }
        }
        skin.inset(g, leftPos + LIST_X - 2, topPos + DET_Y, imageWidth - 12, DET_H);
        if (sel != null)
            skin.slot(g, leftPos + BlueprintTableMenu.SAMPLE_X, topPos + BlueprintTableMenu.SAMPLE_Y);
        if (!ClientSettings.reduceMotion()) { // grid shimmer: faint ruled lines that breathe out of step
            float t = GuiStyle.time();
            for (int x = LIST_X; x < imageWidth - 12; x += 12) {
                int a = (int) (10 + 10 * Math.sin(t * 0.08f + x * 0.35f));
                g.fill(leftPos + x, topPos + ROW_Y - 1, leftPos + x + 1, topPos + ROW_Y + VISIBLE * ROW_H + 1, a << 24 | 0xFFFFFF);
            }
        }
        scrollbar(g, leftPos + imageWidth - 7, topPos + ROW_Y, VISIBLE * ROW_H, items().size(), VISIBLE, rowScroll);
    }

    /** {@code text} cut with an ellipsis to {@code w}. */
    private void fit(GuiGraphics g, String s, int x, int y, int w, int colour) {
        g.drawString(font, font.width(s) <= w ? s : font.plainSubstrByWidth(s, w - font.width("...")) + "...", x, y, colour, false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        ModularType t = current();
        if (t == null)
            return;
        List<String> items = visibleItems();
        int right = imageWidth - 16;
        for (int i = 0; i < items.size(); i++) {
            String role = items.get(i);
            int y = ROW_Y + i * ROW_H + 5;
            if (role.equals(ADD)) {
                Component add = Component.translatable("gui.feedback.blueprint_table.add_part");
                g.drawString(font, add, (imageWidth - font.width(add)) / 2, y, skin.dim(), false);
                continue;
            }
            String sum = summary(role);
            int sw = Math.min(font.width(sum), 140);
            fit(g, sum, right - sw, y, 140, skin.dim());
            String key = keyOf(role);
            if (t.ranged(role) || (key != null && swaps(key).size() > 1))
                continue; // its name is its stepper
            int x = NAME_X + (key != null && removable(key) ? 15 : 0);
            fit(g, key != null ? keyLabel(key) : roleName(role).getString(), x, y, right - sw - x - 6, skin.ink());
        }
        String role = sel();
        if (role != null) {
            int y = DET_Y + 4;
            g.drawString(font, Component.translatable("gui.feedback.blueprint_table.material"), NAME_X, y + 5, skin.dim(), false);
            y += DET_ROW;
            if (shapes(role).size() > 1) {
                g.drawString(font, Component.translatable("gui.feedback.blueprint_table.shape"), NAME_X, y + 5, skin.dim(), false);
                y += DET_ROW;
            }
            if (edged() && role.equals(t.wearRole()))
                g.drawString(font, Component.translatable("gui.feedback.blueprint_table.edge"), NAME_X, y + 5, skin.dim(), false);
        }
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
        int top = STATS_Y;
        g.drawString(font, Component.translatable("gui.feedback.blueprint_table.as_drawn"), 8, top, skin.dim(), false);
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
            g.drawString(font, hint, changeBox[0], changeBox[1], skin.dim(), false);
        }
    }

    /** Hovering a control or a part says what it is, a modifier what it means, a figure's line what it is for the item. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (addPopup.active())
            return;
        super.renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && hoveredSlot.hasItem())
            return;
        for (Tip t : tips) {
            if (mouseX >= t.x && mouseX < t.x + t.w && mouseY >= t.y && mouseY < t.y + t.h) {
                List<Component> lines = t.lines.get();
                if (!lines.isEmpty())
                    g.renderTooltip(font, lines.stream().flatMap(c -> font.split(c, t.width).stream()).toList(), mouseX, mouseY);
                return;
            }
        }
        int mx = mouseX - leftPos, my = mouseY - topPos;
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
