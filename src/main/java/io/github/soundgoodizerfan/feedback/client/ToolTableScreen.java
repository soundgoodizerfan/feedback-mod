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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.modular.ForgeRhythm;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.Station;
import io.github.soundgoodizerfan.feedback.modular.ToolTableAssembly;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.net.ForgeBeginPayload;
import io.github.soundgoodizerfan.feedback.net.ForgeFinishPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Tool Table, Gun Table and Prep Table: three tabs on one bench, each table on its own skin (stone, panel,
 * brick). At the Prep Table the tabs are Parts (the containers, made with the knife), Assemble (with a dish type
 * picker over the left column) and Prepare (one food in the work slot, cut with the knife or finished by pouring).
 *
 * <p>The Tool Table: three tabs on one bench. <b>Parts</b> lists every shape a part can be made in
 * (scroll, click), takes its stock in cells and gives the part; a forged shape is played here, as a
 * rhythm (spec 4.1). <b>Assemble</b> takes parts in six slots and names what they make, from the
 * parts. <b>Rework</b> takes a finished tool and mends it, swaps a part, adds a tip or binding, or
 * takes it apart. No figure is shown: this is a bench, not a drawing board (the Blueprint Table
 * predicts, instruments measure).
 *
 * <h2>The forging screen</h2>
 * Ported from the hand hammer's own screen, which the table replaced: <b>tap</b> on the left lane (left
 * click or Z) and <b>heavy</b> on the right (right click or X) as notes fall to a hit line, the
 * anvil ringing each blow, a metronome clicking every step after a four-click count-in. A note that
 * falls while the stock is out of its window is dim and dead, and the stock's own heat is shown as a
 * glow behind the part -- not an instrument, just what a smith sees. The screen only plays: it
 * sends the blows and the server judges them; the result shown is the client's own judging of the
 * same hits, seeded by the hits themselves, so the two agree.
 */
public class ToolTableScreen extends SkinnedScreen<ToolTableMenu> {

    static final int LIST_X = 10, LIST_Y = 40, LIST_W = 114, ROW_H = 11, ROWS = 13;
    /** The part list sits under its search box: one row shorter, starting lower. */
    static final int PART_Y = LIST_Y + 16, PART_ROWS = 11;

    /** What this table is: skin, tabs, labels and help keys all come from here. */
    private final Station st;

    /** The work panels this station lists, in order; they keep their state across the screen's resizes. */
    private final List<WorkPanel> panels = new ArrayList<>();
    private float partial;

    private AbstractWidget tabs;
    private Button apart;
    private List<ToolTableShaping.Option> options = List.of();
    private int optionsGeneration = -1;
    /** The search box over the part list, and the indices into {@code options} it lets through. */
    private net.minecraft.client.gui.components.EditBox search;
    private List<Integer> shown = List.of();
    /** Options of each part, standard shape first; a part with several is a collapsible group. */
    private final java.util.Map<ResourceLocation, List<Integer>> groups = new java.util.LinkedHashMap<>();
    private final java.util.Set<ResourceLocation> open = new java.util.HashSet<>();
    private boolean searching;
    /** How many shapes the stock in the cells can make here, before the search box narrows them. */
    private int offeredCount;
    private int scroll;
    /** The left column's text box in the other tabs: how far it is scrolled, in lines. */
    private int boxScroll;
    /** When the row under the pointer, or the chosen one, began to be read: the marquee starts from here. */
    private long marqueeFrom;
    private int marqueeRow = -1;

    // What the contents read as, recomputed when they change rather than every frame.
    private int readKey = Integer.MIN_VALUE;
    private Component status = Component.empty();
    private boolean canForge;

    // Feel (docs/gui_flow_feel.md): what the last contents read as, when the slots last shook, and the stage move running.
    private static final java.util.Set<String> REJECTS = java.util.Set.of("bad_stock", "cold", "unmovable", "nothing_rework", "bad_food", "short_saw");
    /** The preview rectangle, relative to the screen: the result, and the work area in its place. */
    private static final int PX = ToolTableMenu.GRID_X - 4, PY = ToolTableMenu.TOOL_Y - 4, PW = ToolTableMenu.W - ToolTableMenu.GRID_X - 6,
            PH = ToolTableMenu.INV_Y - ToolTableMenu.TOOL_Y - 14;
    private int lastStage = -1, lastFilled = -1;
    private String lastWhy = "";
    private float shakeAt = Float.NEGATIVE_INFINITY, stageAt = Float.NEGATIVE_INFINITY;
    private GuiSkin.Move move = GuiSkin.Move.NONE;
    private AbstractWidget strip;
    /** The rework tab's part rows: what the tool in the tool slot is made of, and the roles it has room for. */
    private record Row(Component text, Component tip, boolean empty) { }
    private List<Row> rows = List.of();

    public ToolTableScreen(ToolTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.valueOf(Station.of(menu).skin()));
        st = Station.of(menu);
        imageWidth = ToolTableMenu.W;
        imageHeight = ToolTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 6;
        inventoryLabelX = ToolTableMenu.INV_X;
        inventoryLabelY = ToolTableMenu.INV_Y - 11;
        if (st.has(Station.Panel.FORGE))
            panels.add(new ForgePanel(this));
        if (st.has(Station.Panel.CUT))
            panels.add(new CutPanel(this));
        if (st.has(Station.Panel.PREP))
            panels.add(new PrepPanel(this));
        if (st.has(Station.Panel.ASSEMBLE))
            panels.add(new AssemblePanel(this));
    }

    // What the panels may ask of the frame.
    ToolTableMenu menu() {
        return menu;
    }

    Station station() {
        return st;
    }

    int left() {
        return leftPos;
    }

    int top() {
        return topPos;
    }

    int width() {
        return imageWidth;
    }

    net.minecraft.client.gui.Font font() {
        return font;
    }

    float partial() {
        return partial;
    }

    boolean canForge() {
        return canForge;
    }

    /** Plays the skin's cue and, for a stage change, start or abort, starts its move over the preview rectangle. */
    GuiSkin.Move cue(GuiSkin.Cue cue, Material.Kind kind, float bias) {
        GuiSkin.Move m = skin.cue(cue, kind, bias);
        if (m.style() != GuiSkin.Style.NONE) {
            move = m;
            stageAt = GuiStyle.time();
        }
        return m;
    }

    /** The kind of the stock in the cells, or null; the cues' pitch follows it. */
    private Material.Kind stockKind() {
        return level() == null ? null : menu.stuff(level()).flatMap(l -> l.material().kind()).orElse(null);
    }

    <W extends net.minecraft.client.gui.components.AbstractWidget> W add(W widget) {
        return addRenderableWidget(widget);
    }

    /**
     * Where the player is in the four stages (form, stock, result, work), read off what the menu holds: work while a panel
     * plays, result once the output slot has something, otherwise form until a shape is picked (the part list, the cut grid)
     * and stock after it. The tabs without a form to pick (assemble, rework, prepare, smith) are in stock.
     */
    private int stage() {
        if (busy())
            return 3;
        if (menu.getSlot(ToolTableMenu.OUT).hasItem())
            return 2;
        int mode = menu.mode();
        if (mode == ToolTableMenu.MODE_PART)
            return menu.picked() < 0 ? 0 : 1;
        return mode == ToolTableMenu.MODE_CUT && menu.picked() < 0 && !menu.stock().get(0).isEmpty() ? 0 : 1;
    }

    /**
     * A click on a finished stage of the strip. Only the form can be stepped back to: pressing the current tab's own button clears
     * the chosen shape and keeps the stock. Stock and result are derived from the cells, so there is nothing to rewind.
     */
    private void back(int i) {
        if (busy() || i >= stage() || i != 0 || menu.picked() < 0)
            return;
        for (Station.Tab t : st.tabs())
            if (t.mode() == menu.mode()) {
                cue(GuiSkin.Cue.STAGE, null, 1f);
                press(t.button());
                return;
            }
    }

    /** Whether some panel is mid-work. */
    boolean busy() {
        for (WorkPanel p : panels)
            if (p.busy())
                return true;
        return false;
    }

    @Override
    protected void init() {
        super.init();
        List<Station.Tab> shownTabs = st.tabs();
        tabs = addRenderableWidget(GuiStyle.tabs(skin, leftPos + 10, topPos + 18, shownTabs.size() > 2 ? 240 : 120, 16,
                shownTabs.stream().<Component>map(t -> Component.translatable(t.key())).toList(),
                () -> {
                    for (int i = 0; i < shownTabs.size(); i++)
                        if (shownTabs.get(i).mode() == menu.mode())
                            return i;
                    return 0;
                }, i -> press(shownTabs.get(i).button())));
        apart = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.tool_table.apart"),
                b -> press(ToolTableMenu.BTN_APART), leftPos + ToolTableMenu.TOOL_X + 24, topPos + ToolTableMenu.TOOL_Y, 70, 16));
        apart.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.feedback.tool_table.apart.tip")));
        // A finished stage can be clicked back to, never a later one (see back()); the lamps over it are drawn in render().
        strip = addRenderableWidget(GuiStyle.tabs(skin, leftPos + imageWidth - 12 - 130, topPos + 5, 130, 11,
                List.of("form", "stock", "result", "work").stream().<Component>map(k -> Component.translatable("gui.feedback.tool_table.stage." + k)).toList(),
                this::stage, this::back));
        for (WorkPanel p : panels)
            p.init();
        search = addRenderableWidget(new net.minecraft.client.gui.components.EditBox(font, leftPos + LIST_X - 1, topPos + LIST_Y - 2,
                LIST_W + 2, 14, Component.translatable("gui.feedback.tool_table.search")));
        search.setHint(Component.translatable("gui.feedback.tool_table.search").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        search.setMaxLength(32);
        search.setResponder(text -> {
            refilter();
            scroll = 0;
        });
        refreshOptions();
    }

    /** The shapes whose name holds what was typed, in any case. */
    private void refilter() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<Integer> out = new ArrayList<>();
        searching = !q.isEmpty();
        groups.clear();
        // Nothing is offered until a material is in the cells, and then only the shapes that material can make here.
        Optional<Material.Located> stuff = level() == null ? Optional.empty() : menu.stuff(level());
        List<Integer> offered = new ArrayList<>();
        for (int i = 0; i < options.size(); i++)
            if (stuff.isPresent() && ToolTableShaping.takes(st, options.get(i), stuff.get()))
                offered.add(i);
        offeredCount = offered.size();
        for (int i : offered)
            groups.computeIfAbsent(options.get(i).shaping().item(), k -> new ArrayList<>()).add(i);
        for (int i : offered) {
            if (searching ? options.get(i).label().getString().toLowerCase(Locale.ROOT).contains(q)
                    : isHeader(i) || !isGrouped(i) || open.contains(options.get(i).shaping().item()))
                out.add(i);
        }
        shown = out;
        scroll = Mth.clamp(scroll, 0, Math.max(0, shown.size() - PART_ROWS));
    }

    private boolean isGrouped(int i) {
        return groups.get(options.get(i).shaping().item()).size() > 1;
    }

    /** The first option of a part with variants: its row carries the fold arrow. */
    private boolean isHeader(int i) {
        return isGrouped(i) && groups.get(options.get(i).shaping().item()).get(0) == i;
    }

    void press(int id) {
        if (Minecraft.getInstance().gameMode != null)
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
    }


    net.minecraft.world.level.Level level() {
        return Minecraft.getInstance().level;
    }

    private void refreshOptions() {
        if (optionsGeneration == ModularTypes.generation() || level() == null)
            return;
        options = ToolTableShaping.options(menu.shapes(level()));
        optionsGeneration = ModularTypes.generation();
        refilter();
    }

    /** The shape chosen, from the list this screen already holds. */
    Optional<ToolTableShaping.Option> current() {
        return menu.picked() >= 0 && menu.picked() < options.size() ? Optional.of(options.get(menu.picked())) : Optional.empty();
    }

    // --- what the contents read as -------------------------------------------------------------

    private void read() {
        var level = level();
        if (level == null)
            return;
        List<ItemStack> all = new ArrayList<>();
        for (int i = ToolTableMenu.HAMMER; i < menu.inv(); i++)
            all.add(menu.getSlot(i).getItem());
        int key = java.util.Objects.hash(menu.mode(), menu.picked(), menu.typePicked(level), ItemStack.hashStackList(all), ModularTypes.generation(),
                ClientMaterials.get().size(), menu.getSlot(ToolTableMenu.OUT).getItem().getCount());
        if (key == readKey)
            return;
        readKey = key;
        refilter();
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = menu.types(level);
        boolean out = menu.getSlot(ToolTableMenu.OUT).hasItem();
        canForge = false;
        for (WorkPanel p : panels)
            p.read(level, materials);
        String why;
        boolean saw = st.saws();
        if (menu.mode() == ToolTableMenu.MODE_SMITH) {
            why = out ? "ready" : menu.stock().get(0).isEmpty() ? "put_smith" : "nothing_smith";
        } else if (menu.mode() == ToolTableMenu.MODE_CUT) {
            why = out ? "ready" : menu.stock().get(0).isEmpty() ? (saw ? "put_saw" : "put_cut") : !menu.hasHammer() ? "no_axe"
                    : saw && menu.picked() >= 0 ? "short_saw" : "pick_cut";
        } else if (!menu.hasHammer() && !(st.dishes() && menu.mode() == ToolTableMenu.MODE_PREPARE)) {
            why = saw ? "no_axe" : "no_hammer";
        } else if (out) {
            why = "ready";
        } else if (menu.mode() == ToolTableMenu.MODE_PART) {
            Optional<ToolTableShaping.Option> option = current();
            Optional<ToolTableShaping.Stock> stock = option.flatMap(o -> ToolTableShaping.check(st, o, menu.stock(), materials, level));
            canForge = stock.isPresent() && option.get().forged() && ForgeRhythm.workable(stock.get().what().material(), stock.get().coldest())
                    && ForgeRhythm.movable(option.get().variant().rhythm(), menu.strikes(), stock.get().what().material());
            boolean any = menu.stock().stream().anyMatch(s -> !s.isEmpty());
            why = option.isEmpty() ? "no_shape" : canForge ? "forge" : stock.isPresent() && option.get().forged()
                    ? (ForgeRhythm.workable(stock.get().what().material(), stock.get().coldest()) ? "unmovable" : "cold")
                    : !any ? "put_stock" : "bad_stock";
        } else if (menu.mode() == ToolTableMenu.MODE_ASSEMBLE) {
            why = menu.assembly().stream().allMatch(ItemStack::isEmpty) ? "put_parts" : "nothing";
        } else if (st.dishes()) {
            ItemStack work = menu.work();
            why = work.isEmpty() ? "put_food" : menu.preparable(level, 2) ? "dough" : menu.preparable(level, 0) ? "cut" : menu.preparable(level, 1) ? "finish"
                    : ToolTableMenu.isDish(work) ? (ToolTableMenu.finished(level, work) ? "has_finish" : "put_seasoning")
                    : menu.hasHammer() ? "bad_food" : "no_knife";
        } else {
            why = menu.tool().isEmpty() ? "put_tool" : "nothing_rework";
        }
        status = Component.translatable(st.status(why));
        rows = List.of();
        if (menu.mode() == ToolTableMenu.MODE_REWORK && !st.dishes()) {
            ItemStack tool = menu.tool();
            var design = ModularStacks.design(tool);
            ModularType type = design == null ? null : types.get(design.type());
            if (type != null) {
                List<Row> built = new ArrayList<>();
                boolean worn = ModularStacks.worn(tool) > 0;
                for (String r : type.order()) {
                    Component role = Component.translatable("modular.feedback.role." + type.roles().get(r).name());
                    var part = design.part(r);
                    built.add(part == null
                            ? new Row(Component.translatable("gui.feedback.tool_table.row.empty", role),
                                    Component.translatable("gui.feedback.tool_table.tip.row_add", role), true)
                            : new Row(Component.translatable("gui.feedback.tool_table.row", role,
                                    Component.translatable("material." + part.material().getNamespace() + "." + part.material().getPath())),
                                    Component.translatable(worn ? "gui.feedback.tool_table.tip.row_mend" : "gui.feedback.tool_table.tip.row_swap"), false));
                }
                rows = built;
            }
        }
        int filled = 0;
        for (ItemStack s : all)
            if (!s.isEmpty())
                filled++;
        if (lastFilled >= 0) {
            if (REJECTS.contains(why) && !why.equals(lastWhy)) {
                skin.cue(GuiSkin.Cue.REJECT, stockKind(), 1f);
                shakeAt = GuiStyle.time();
            } else if (filled > lastFilled)
                skin.cue(GuiSkin.Cue.SEAT, stockKind(), 1f);
        }
        lastFilled = filled;
        lastWhy = why;
    }

    // --- the forging screen ---------------------------------------------------------------------

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshOptions();
        read();
        boolean part = menu.mode() == ToolTableMenu.MODE_PART;
        boolean playing = busy();
        // The search box is for a long list only: with a few shapes on offer it is clutter.
        search.visible = part && !playing && (offeredCount > PART_ROWS || !search.getValue().isEmpty());
        search.setEditable(search.visible);
        if (!search.visible)
            search.setFocused(false);
        tabs.active = strip.active = !playing;
        int stage = stage();
        if (lastStage >= 0 && stage > lastStage)
            cue(stage == 3 ? GuiSkin.Cue.START : GuiSkin.Cue.STAGE, stockKind(), 1f);
        lastStage = stage;
        apart.visible = menu.mode() == ToolTableMenu.MODE_REWORK && !playing && !st.dishes();
        apart.active = menu.hasHammer() && ModularStacks.design(menu.tool()) != null;
        for (WorkPanel p : panels)
            p.tick();
    }

    /** Taking the result is the commit: a finish cue, pitched by the stock. */
    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        boolean take = slot != null && slot.index == ToolTableMenu.OUT && slot.hasItem();
        super.slotClicked(slot, slotId, button, type);
        if (take)
            cue(GuiSkin.Cue.FINISH, stockKind(), 1f);
    }

    // --- input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (WorkPanel p : panels)
            if (p.click(mouseX, mouseY, button))
                return true;
        if (settled() && menu.mode() == ToolTableMenu.MODE_PART && button == 1 && !searching) {
            double lx = mouseX - leftPos - LIST_X, ly = mouseY - topPos - PART_Y;
            int row = (int) Math.floor(ly / ROW_H);
            if (lx >= 0 && lx < LIST_W && ly >= 0 && row < PART_ROWS && scroll + row < shown.size()
                    && isHeader(shown.get(scroll + row))) {
                GuiSkin.play(skin.click(), 1f);
                ResourceLocation item = options.get(shown.get(scroll + row)).shaping().item();
                if (!open.remove(item))
                    open.add(item);
                refilter();
                return true;
            }
        }
        if (settled() && menu.mode() == ToolTableMenu.MODE_PART && button == 0) {
            double lx = mouseX - leftPos - LIST_X, ly = mouseY - topPos - PART_Y;
            int row = (int) Math.floor(ly / ROW_H);
            if (lx >= 0 && lx < LIST_W && ly >= 0 && row < PART_ROWS && scroll + row < shown.size()) {
                GuiSkin.play(skin.click(), 1f);
                int i = shown.get(scroll + row);
                if (!searching && isHeader(i) && lx < 11) {
                    ResourceLocation item = options.get(i).shaping().item();
                    if (!open.remove(item))
                        open.add(item);
                    refilter();
                } else
                    press(ToolTableMenu.BTN_PICK + i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        boolean part = menu.mode() == ToolTableMenu.MODE_PART;
        int top = part ? PART_Y : LIST_Y, rows = part ? PART_ROWS : ROWS;
        if (!busy() && mouseX >= leftPos + LIST_X && mouseX < leftPos + LIST_X + LIST_W + 6
                && mouseY >= topPos + top && mouseY < topPos + top + rows * ROW_H) {
            if (part)
                scroll = Mth.clamp(scroll - (int) Math.signum(scrollY), 0, Math.max(0, shown.size() - PART_ROWS));
            else
                boxScroll = Math.max(0, boxScroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        // A focused search box takes every key, or typing an inventory key would close the table.
        if (search != null && search.visible && search.isFocused()) {
            if (key == InputConstants.KEY_ESCAPE)
                search.setFocused(false);
            else
                search.keyPressed(key, scan, modifiers);
            return true;
        }
        for (WorkPanel p : panels)
            if (p.key(key, scan, modifiers))
                return true;
        return super.keyPressed(key, scan, modifiers);
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        idle(g);
        int shake = GuiStyle.shake(shakeAt, 2);
        for (int i = 0; i < menu.inv(); i++) {
            Slot s = menu.getSlot(i);
            if (!s.isActive())
                continue;
            if (i == ToolTableMenu.OUT) {
                skin.plaque(g, leftPos + s.x - 5, topPos + s.y - 5, 26, 26);
                if (s.hasItem()) {
                    float pulse = 0.5f + 0.5f * Mth.sin(net.minecraft.Util.getMillis() / 260f);
                    int a = 0x60 + (int) (pulse * 0x9F);
                    g.renderOutline(leftPos + s.x - 2, topPos + s.y - 2, 20, 20, a << 24 | skin.accent() & 0xFFFFFF);
                    g.renderOutline(leftPos + s.x - 4, topPos + s.y - 4, 24, 24, (a / 3) << 24 | skin.accent() & 0xFFFFFF);
                }
            }
            skin.slot(g, leftPos + s.x + (i > ToolTableMenu.HAMMER ? shake : 0), topPos + s.y);
        }
        // An empty tool slot pulses at the rework tab: it is where to start.
        if (menu.mode() == ToolTableMenu.MODE_REWORK && !st.dishes() && !menu.getSlot(ToolTableMenu.TOOL).hasItem()) {
            Slot t = menu.getSlot(ToolTableMenu.TOOL);
            int a = 0x40 + (int) ((0.5f + 0.5f * Mth.sin(GuiStyle.time() * 0.25f)) * 0xA0);
            g.renderOutline(leftPos + t.x - 2, topPos + t.y - 2, 20, 20, a << 24 | skin.accent() & 0xFFFFFF);
        }
        inventory(g, ToolTableMenu.INV_X, ToolTableMenu.INV_Y);
        int hx = leftPos + ToolTableMenu.HAMMER_X, hy = topPos + ToolTableMenu.HAMMER_Y;
        Component hammer = Component.translatable(st.toolLabel()), result = Component.translatable("gui.feedback.tool_table.result");
        if (menu.getSlot(ToolTableMenu.HAMMER).isActive())
            g.drawString(font, hammer, hx + 8 - font.width(hammer) / 2, hy - 11, skin.dim(), false);
        g.drawString(font, result, leftPos + ToolTableMenu.OUT_X + 8 - font.width(result) / 2, topPos + ToolTableMenu.OUT_Y - 12, skin.dim(), false);
        WorkPanel owner = owner(menu.mode());
        if (menu.mode() == ToolTableMenu.MODE_PART)
            list(g, mouseX, mouseY);
        else if (owner != null)
            owner.body(g, mouseX, mouseY);
        else if (menu.mode() == ToolTableMenu.MODE_REWORK)
            rework(g, mouseX, mouseY);
        int sx = leftPos + ToolTableMenu.GRID_X;
        // Below whatever the tab puts under the slots: the plan slot, the Prep Table's two assembly rows, or its four buttons.
        int rows = menu.mode() == ToolTableMenu.MODE_SMITH || menu.mode() == ToolTableMenu.MODE_CUT ? 1 : 3;
        int sy = topPos + (owner != null && owner.statusY() >= 0 ? owner.statusY()
                : ToolTableMenu.GRID_Y + (st.statusDrop() > 0 ? st.statusDrop() : rows * 18 + 8));
        boolean ok = menu.getSlot(ToolTableMenu.OUT).hasItem() || canForge;
        int line = 0;
        for (var l : font.split(status, ToolTableMenu.W - ToolTableMenu.GRID_X - 14)) {
            g.drawString(font, l, sx, sy + line * 10, ok ? skin.accent() : skin.dim(), false);
            line++;
        }
        if (menu.mode() == ToolTableMenu.MODE_PART)
            fit(g, selection().getString(), leftPos + ToolTableMenu.TOOL_X, topPos + ToolTableMenu.TOOL_Y + 2,
                    ToolTableMenu.W - ToolTableMenu.TOOL_X - 14, skin.ink(), false, 0);
        else if (owner != null)
            owner.labels(g);
        else if (menu.mode() == ToolTableMenu.MODE_SMITH)
            for (int i = 0; i < 3; i++)
                g.drawString(font, Component.translatable("gui.feedback.tool_table.smith." + i), leftPos + ToolTableMenu.GRID_X + i * 18 + 8
                        - font.width(Component.translatable("gui.feedback.tool_table.smith." + i)) / 2, topPos + ToolTableMenu.GRID_Y - 11, skin.dim(), false);
        else
            g.drawString(font, Component.translatable("gui.feedback.tool_table.stock"), leftPos + ToolTableMenu.GRID_X, topPos + ToolTableMenu.GRID_Y - 11, skin.dim(), false);
    }

    /** An empty slot says what it takes when the pointer rests on it. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (busy())
            return;
        for (WorkPanel p : panels)
            if (p.tooltip(g, mouseX, mouseY))
                return;
        if (menu.mode() == ToolTableMenu.MODE_REWORK && !st.dishes()) {
            int row = rowAt(mouseX, mouseY);
            if (row >= 0) {
                g.renderTooltip(font, font.split(rows.get(row).tip(), 190), mouseX, mouseY);
                return;
            }
        }
        if (menu.mode() == ToolTableMenu.MODE_PART) {
            int hovered = optionAt(mouseX, mouseY);
            if (hovered >= 0) {
                g.renderTooltip(font, wrap(PartPresenter.describe(options.get(hovered))), mouseX, mouseY);
                return;
            }
        }
        Slot s = hoveredSlot;
        if (s != null && s.isActive() && !s.hasItem() && s.index >= ToolTableMenu.HAMMER && s.index < menu.inv()
                && !(s.index >= ToolTableMenu.STOCK && s.index < ToolTableMenu.TOOL)) {
            String key = st.tip(s.index == ToolTableMenu.HAMMER ? "hammer" : s.index == ToolTableMenu.TOOL ? "tool" : s.index == ToolTableMenu.PLAN ? "plan"
                    : s.index == menu.inv() - 1 && st.gun() ? "round" : "part");
            g.renderTooltip(font, font.split(Component.translatable("gui.feedback.tool_table.tip." + key, ToolTableAssembly.CAPACITY), 190), mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    /** What the chosen shape asks for, over the stock cells. */
    private Component selection() {
        return current().<Component>map(o -> Component.translatable(st.dishes() ? "gui.feedback.prep_table.takes" : "gui.feedback.tool_table.takes", o.cells()))
                .orElse(Component.empty());
    }

    /** The panel whose tab {@code mode} is, if any. */
    private WorkPanel owner(int mode) {
        for (WorkPanel p : panels)
            if (p.owns(mode))
                return p;
        return null;
    }

    /** The part row under the pointer in the rework tab, or -1. */
    private int rowAt(int mouseX, int mouseY) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y;
        int r = (mouseY - y) / ROW_H;
        return mouseX >= x && mouseX < x + LIST_W && mouseY >= y && r >= 0 && r < rows.size() && r < ROWS ? r : -1;
    }

    /** What the tool is made of, one row per role; an empty role has room for a part. No prose: each row's tooltip says what to do. */
    private void rework(GuiGraphics g, int mouseX, int mouseY) {
        if (rows.isEmpty())
            return;
        int x = leftPos + LIST_X, y = topPos + LIST_Y, n = Math.min(rows.size(), ROWS), hovered = rowAt(mouseX, mouseY);
        skin.inset(g, x - 2, y - 2, LIST_W + 4, n * ROW_H + 4);
        for (int r = 0; r < n; r++) {
            skin.row(g, x, y + r * ROW_H, LIST_W, ROW_H, false, r == hovered);
            fit(g, rows.get(r).text().getString(), x + 2, y + r * ROW_H + 2, LIST_W - 4, rows.get(r).empty() ? skin.dim() : skin.ink(), false, 0);
        }
    }

    /**
     * A short list of lines (figures, never prose), wrapped to the box and clipped to it by a scissor; scrolls with the wheel
     * when the list is long.
     */
    void column(GuiGraphics g, List<Component> text, int below) {
        int x = leftPos + LIST_X, y = topPos + LIST_Y + below, h = ROWS * ROW_H - below;
        List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : text)
            wrapped.addAll(font.split(c, LIST_W - 4));
        int visible = h / 10;
        boxScroll = Mth.clamp(boxScroll, 0, Math.max(0, wrapped.size() - visible));
        skin.inset(g, x - 2, y - 2, LIST_W + 4, h + 4);
        g.enableScissor(x, y, x + LIST_W, y + h);
        for (int i = boxScroll; i < wrapped.size() && i < boxScroll + visible + 1; i++)
            g.drawString(font, wrapped.get(i), x + 2, y + (i - boxScroll) * 10, skin.ink(), false);
        g.disableScissor();
        scrollbar(g, x + LIST_W + 2, y, h, wrapped.size(), visible, boxScroll);
    }

    /**
     * {@code text} in a box {@code w} wide. Over-long text slides to show its end, pauses, and
     * slides back, clipped to the box, when {@code live}; otherwise it is cut with an ellipsis.
     */
    private void fit(GuiGraphics g, String text, int x, int y, int w, int colour, boolean live, long since) {
        int width = font.width(text);
        if (width <= w) {
            g.drawString(font, text, x, y, colour, false);
            return;
        }
        if (!live) {
            g.drawString(font, font.plainSubstrByWidth(text, w - font.width("...")) + "...", x, y, colour, false);
            return;
        }
        int over = width - w;
        long t = net.minecraft.Util.getMillis() - since;
        long pause = 700, slide = over * 25L, back = 250, cycle = pause + slide + pause + back;
        t %= cycle;
        float offset = t < pause ? 0 : t < pause + slide ? (t - pause) / (float) slide * over
                : t < pause + slide + pause ? over : over * (1 - (t - pause - slide - pause) / (float) back);
        g.enableScissor(x, y - 1, x + w, y + 10);
        g.drawString(font, text, x - Math.round(offset), y, colour, false);
        g.disableScissor();
    }

    /** The list row under the pointer, or the chosen shape's name beside the stock, or -1. */
    private int optionAt(int mouseX, int mouseY) {
        int x = leftPos + LIST_X, y = topPos + PART_Y;
        if (mouseX >= x && mouseX < x + LIST_W && mouseY >= y && mouseY < y + PART_ROWS * ROW_H) {
            int i = scroll + (mouseY - y) / ROW_H;
            return i < shown.size() ? shown.get(i) : -1;
        }
        int nx = leftPos + ToolTableMenu.TOOL_X, ny = topPos + ToolTableMenu.TOOL_Y + 2;
        if (menu.picked() >= 0 && menu.picked() < options.size() && mouseX >= nx && mouseX < leftPos + ToolTableMenu.W - 14
                && mouseY >= ny - 1 && mouseY < ny + 10)
            return menu.picked();
        return -1;
    }

    private List<net.minecraft.util.FormattedCharSequence> wrap(List<Component> lines) {
        List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : lines)
            wrapped.addAll(font.split(c, 190));
        return wrapped;
    }

    private void list(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + LIST_X, y = topPos + PART_Y;
        skin.inset(g, x - 2, y - 2, LIST_W + 4, PART_ROWS * ROW_H + 4);
        for (int r = 0; r < PART_ROWS && scroll + r < shown.size(); r++) {
            int i = shown.get(scroll + r);
            ToolTableShaping.Option o = options.get(i);
            int ry = y + r * ROW_H;
            boolean hover = mouseX >= x && mouseX < x + LIST_W && mouseY >= ry && mouseY < ry + ROW_H;
            skin.row(g, x, ry, LIST_W, ROW_H, i == menu.picked(), hover);
            boolean live = hover || i == menu.picked();
            if (live && marqueeRow != i) {
                marqueeRow = i;
                marqueeFrom = net.minecraft.Util.getMillis();
            }
            int indent = searching || !isGrouped(i) ? 2 : 11;
            if (!searching && isHeader(i))
                g.drawString(font, open.contains(o.shaping().item()) ? "↑" : "↓", x + 2, ry + 2, skin.dim(), false);
            fit(g, o.label().getString(), x + indent, ry + 2, LIST_W - indent - 2, o.forged() ? skin.accent() : skin.ink(), live, marqueeFrom);
        }
        if (shown.isEmpty()) {
            // Empty cells say what to do; stock the bench cannot use says so.
            String why = offeredCount > 0 ? "search.none" : menu.stuff(level()).isPresent() ? "list.nothing" : "list.empty";
            int line = 0;
            for (var l : font.split(Component.translatable("gui.feedback.tool_table." + why), LIST_W - 4))
                g.drawString(font, l, x + 2, y + 2 + line++ * 10, skin.dim(), false);
        }
        scrollbar(g, x + LIST_W + 2, y, PART_ROWS * ROW_H, shown.size(), PART_ROWS, scroll);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        partial = partialTick;
        super.render(g, mouseX, mouseY, partialTick);
        if (!settled())
            return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (busy())
            for (WorkPanel p : panels)
                p.overlay(g);
        // The stage move covers the preview rectangle, above the work area.
        GuiStyle.transition(g, skin, move, stageAt, leftPos + PX, topPos + PY, PW, PH);
        g.pose().translate(0, 0, 50);
        lamps(g);
        g.pose().popPose();
    }

    /** A bar under each stage of the strip: lit from the first to the current stage (finished stays lit), the current one breathing. */
    private void lamps(GuiGraphics g) {
        int stage = stage(), x = strip.getX(), y = strip.getY() + strip.getHeight() - 2, seg = strip.getWidth() / 4;
        float pulse = 0.65f + 0.35f * Mth.sin(GuiStyle.time() * 0.3f);
        for (int i = 0; i < 4; i++) {
            int cx = x + i * seg + seg / 2;
            int c = i > stage ? 0x50000000 | skin.dim() & 0xFFFFFF
                    : (i == stage && !ClientSettings.reduceMotion() ? (int) (pulse * 255) << 24 : 0xFF000000) | skin.accent() & 0xFFFFFF;
            g.fill(cx - 6, y, cx + 6, y + 1, c);
        }
    }

    /** Each skin's idle life in the preview rectangle: a few 1-2 px particles from a hash of their index, no state, no allocation. */
    private void idle(GuiGraphics g) {
        if (ClientSettings.reduceMotion())
            return;
        int x = leftPos + PX, y = topPos + PY;
        float t = GuiStyle.time();
        switch (skin) {
            case FORGE -> {
                float heat = Mth.clamp(menu.heat(level()) / 900f, 0.2f, 1f);
                drift(g, x, y, t, 4 + (int) (8 * heat), 0xFF8A30, true, 3, 2, (int) (200 * heat));
            }
            case WOOD -> drift(g, x, y, t, 7, 0xEBD9AE, false, 5, 1, 120);
            case STONE -> drift(g, x, y, t, 6, 0x9C9890, false, 0, 1, 130);
            case BRICK -> {
                if (!menu.work().isEmpty())
                    drift(g, x, y, t, 5, 0xFFFFFF, true, 2, 2, 70);
            }
            case PANEL -> {
                float breath = 0.5f + 0.5f * Mth.sin(t * 0.12f);
                // A rare relay flicker: some 3-tick buckets dip.
                int bucket = (int) (t / 3) * 0x9E3779B1;
                int a = (bucket >>> 24) < 3 ? 0x20 : 0x60 + (int) (breath * 0x70);
                g.fill(x + PW - 6, y + 3, x + PW - 3, y + 6, a << 24 | 0xFFB030);
            }
            default -> { }
        }
    }

    /** {@code n} particles drifting up or down the preview rectangle, each fading in and out over its own period. */
    private static void drift(GuiGraphics g, int x, int y, float t, int n, int rgb, boolean rise, int sway, int size, int maxAlpha) {
        for (int i = 0; i < n; i++) {
            int s = (i + 1) * 0x9E3779B1;
            s ^= s >>> 15;
            int period = 50 + (s >>> 4 & 31);
            float f = ((t + (s & 0xFF)) % period) / period;
            int a = (int) (maxAlpha * Mth.sin(f * Mth.PI));
            if (a < 8)
                continue;
            int px = x + (s >>> 8 & 0xFF) * PW / 256 + Math.round(Mth.sin(f * 6.28f + i) * sway);
            int py = y + Math.round((rise ? 1 - f : f) * PH);
            g.fill(px, py, px + size, py + size, a << 24 | rgb);
        }
    }

}
