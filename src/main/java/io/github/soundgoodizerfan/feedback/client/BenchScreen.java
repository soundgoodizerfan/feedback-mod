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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.modular.Station;
import io.github.soundgoodizerfan.feedback.modular.ToolDesigns;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;
import io.github.soundgoodizerfan.feedback.modular.ToolTableShaping;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FormItems;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The bench: the smithing table, the woodworking table, the stonecutter, the Gun Table and the Prep Table are this one screen, each a skin, a hand
 * tool and a set of limits ({@link Station}). {@code docs/gui_bench.md} is the spec; this is the data flow it needs.
 *
 * <pre>
 * What the player does               What the menu holds                       What the fan shows (nodes)
 * ---------------------------------  ----------------------------------------  -----------------------------------------------
 * stock in the centre slot           Centre.STOCK, mode MODE_PART              shapes the stock can become (affordable ones
 *                                    pick = shape index / BTN_CUT + cut index   lit, the rest faint), cuts, break down, and at
 *                                                                              the Prep Table cut/knead; click = make it
 *                                                                              (instant: BTN_PICK then BTN_MAKE; forged: the
 *                                                                              rhythm is played around the stock, ForgePanel)
 * a part in the centre slot          Centre.PART, mode MODE_ASSEMBLE           fitted parts (come off: BTN_OFF), then what the
 *                                    fitted parts in the assembly slots        player holds that attaches (BTN_FIT slot), then
 *                                                                              parts that could attach and would be shaped from
 *                                                                              the stock slot (BTN_SHAPE option); fitting the
 *                                                                              last one makes the tool, which is the centre
 * a tool in the centre slot          Centre.TOOL, mode MODE_REWORK             its parts, one node a role; click one and the
 *                                    stock slot = what mends or swaps          fan offers what the player holds for that role
 *                                                                              (BTN_SWAP role * 256 + slot) and "take off"
 *                                                                              (BTN_OFF role); a tool is a part with others fitted
 * a smithing template in the centre  Centre.TEMPLATE, mode MODE_SMITH          nothing: base and addition go in the stock slots
 * </pre>
 *
 * <p>The player never picks a mode: {@code ToolTableMenu} derives it from the centre on the server and syncs it. The server decides everything
 * (the screen sends button ids and the forging blows); the lists above are computed on both sides from the same menu and are rebuilt
 * only when the contents change. Volume is millibuckets in the menu and shown as ingots here. Numbers never show: bars do.
 *
 * <p>Nothing covers the bench: work happens around the stock ({@link ForgePanel}), motion lives on the objects (the stock settles and glows,
 * a rejected placement shakes the fan, a made part arcs to its slot), and every action has a {@link GuiSkin.Cue} and a visual twin.
 */
public class BenchScreen extends SkinnedScreen<ToolTableMenu> {

    /** The fan's canvas, relative to the panel; the centre slot sits at its middle. */
    static final int VX = 8, VY = 16, VW = 304, VH = 156;
    /** Shapes in a group before they fold into hubs by part. */
    private static final int HUB_AT = 10;

    private final Station st;
    private final ForgePanel forge;
    private final BenchFan fan = new BenchFan();
    private float partial;

    private List<ToolTableShaping.Option> options = List.of();
    private int optionsGeneration = -1;
    private int readKey = Integer.MIN_VALUE;
    private ItemStack seated = ItemStack.EMPTY;
    private int selectedRole = -1;
    private boolean recentre = true;
    private List<Component> sheet = List.of();

    // Feel: when the last reject, seat and make happened, and what is arcing to the output slot.
    private float shakeAt = Float.NEGATIVE_INFINITY, seatAt = Float.NEGATIVE_INFINITY, arcAt = Float.NEGATIVE_INFINITY, saidAt = Float.NEGATIVE_INFINITY;
    private ItemStack arcIcon = ItemStack.EMPTY;
    private Component said = Component.empty();

    // Dragging the fan.
    private boolean panning;

    private Button apart;
    private AbstractWidget typePicker;
    private final float[] bars = new float[3], against = new float[3];

    public BenchScreen(ToolTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, GuiSkin.valueOf(Station.of(menu).skin()));
        st = Station.of(menu);
        imageWidth = ToolTableMenu.W;
        imageHeight = ToolTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 5;
        inventoryLabelX = ToolTableMenu.INV_X;
        inventoryLabelY = ToolTableMenu.INV_Y - 11;
        forge = st.has(Station.Panel.FORGE) ? new ForgePanel(this) : null;
    }

    // What the panels may ask of the bench.
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

    net.minecraft.client.gui.Font font() {
        return font;
    }

    float partial() {
        return partial;
    }

    /** The middle of the fan on the screen: where the stock lies. */
    int cx() {
        return leftPos + VX + VW / 2;
    }

    int cy() {
        return topPos + VY + VH / 2;
    }

    /** Where the centre slot actually is (it follows the fan, clamped to the canvas): every animation that starts at the stock starts here. */
    int slotCx() {
        return leftPos + centreSlot().x + 8;
    }

    int slotCy() {
        return topPos + centreSlot().y + 8;
    }

    private Slot centreSlot() {
        if (centre == null)
            for (Slot s : menu.slots)
                if (s.getContainerSlot() == io.github.soundgoodizerfan.feedback.modular.ToolTableBlockEntity.TOOL) {
                    centre = s;
                    break;
                }
        return centre;
    }

    private Slot centre;

    net.minecraft.world.level.Level level() {
        return Minecraft.getInstance().level;
    }

    void press(int id) {
        if (Minecraft.getInstance().gameMode != null)
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private boolean busy() {
        return forge != null && forge.busy();
    }

    private Material.Kind stockKind() {
        return level() == null ? null : menu.stuff(level()).flatMap(l -> l.material().kind()).orElse(null);
    }

    // --- set up ------------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();
        // The station's folder tabs (data on Station), stood above the panel; none for a station with a single job.
        folderTabs(st.tabs().stream().map(t -> new FolderTabs.Def(Component.translatable(t.key()))).toList(), menu::tab,
                i -> press(ToolTableMenu.BTN_TAB + i));
        apart = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.tool_table.apart"),
                b -> {
                    press(ToolTableMenu.BTN_APART);
                    skin.cue(GuiSkin.Cue.ABORT, stockKind(), 1f);
                }, leftPos + VX + 4, topPos + VY + VH - 18, 66, 14));
        apart.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("gui.feedback.tool_table.apart.tip")));
        if (st.dishes()) {
            typePicker = addRenderableWidget(GuiStyle.stepper(skin, leftPos + VX + 4, topPos + VY + 4, 120, 14, "<", ">", this::typeName, d -> {
                int n = Math.max(1, menu.dishTypes(level()).size());
                press(ToolTableMenu.BTN_TYPE + Math.floorMod(menu.typePicked(level()) + d, n));
            }, true, null));
            typePicker.visible = false;
        }
        readKey = Integer.MIN_VALUE;
    }

    private String typeName() {
        var level = level();
        List<ResourceLocation> types = level == null ? List.of() : menu.dishTypes(level);
        return types.isEmpty() ? "" : PartPresenter.name(types.get(menu.typePicked(level))).getString();
    }

    /** The centre slot rides the fan's middle as it is panned and zoomed (kept inside the canvas so it stays reachable). */
    private void followFan() {
        for (net.minecraft.world.inventory.Slot s : menu.slots) {
            if (s.getContainerSlot() != io.github.soundgoodizerfan.feedback.modular.ToolTableBlockEntity.TOOL)
                continue;
            s.x = Mth.clamp(fan.originX(cx()) - leftPos - 8, VX, VX + VW - 16);
            s.y = Mth.clamp(fan.originY(cy()) - topPos - 8, VY, VY + VH - 16);
            return;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (level() == null)
            return;
        if (optionsGeneration != ModularTypes.generation()) {
            options = ToolTableShaping.options(menu.shapes(level()));
            optionsGeneration = ModularTypes.generation();
            readKey = Integer.MIN_VALUE;
        }
        read();
        if (forge != null)
            forge.tick();
        boolean tool = menu.centre(level()) == ToolTableMenu.Centre.TOOL;
        apart.visible = tool && !busy();
        apart.active = menu.hasHammer();
        if (typePicker != null)
            typePicker.visible = menu.mode() == ToolTableMenu.MODE_ASSEMBLE && !busy();
    }

    /** Rebuild the fan when what it is made from has changed, and give the placement its seat. */
    private void read() {
        List<ItemStack> all = new ArrayList<>();
        for (int i = 0; i < menu.slots.size(); i++)
            all.add(menu.getSlot(i).getItem());
        int key = java.util.Objects.hash(menu.mode(), menu.picked(), menu.cutPicked(), menu.typePicked(level()), menu.tab(), selectedRole, ItemStack.hashStackList(all),
                ModularTypes.generation(), ClientMaterials.get().size());
        if (key == readKey)
            return;
        readKey = key;
        ItemStack centre = menu.centreStack();
        boolean fresh = !ItemStack.isSameItem(centre, seated);
        if (fresh) {
            recentre = true;
            selectedRole = -1;
            if (!centre.isEmpty()) {
                skin.cue(GuiSkin.Cue.SEAT, stockKind(), 1f);
                seatAt = GuiStyle.time();
            }
        }
        seated = centre.copy();
        rebuild();
    }

    // --- the nodes -------------------------------------------------------------------------------

    private void rebuild() {
        var level = level();
        List<BenchFan.Node> list = new ArrayList<>();
        sheet = List.of();
        // The Cut and Saw tabs work stock only; a part or tool in the centre waits for the Parts tab.
        switch (menu.cutTab() && menu.centre(level) != ToolTableMenu.Centre.STOCK ? ToolTableMenu.Centre.EMPTY : menu.centre(level)) {
            case STOCK -> stockNodes(list);
            case PART -> partNodes(list);
            case TOOL -> toolNodes(list);
            default -> { }
        }
        fan.set(list, recentre, VW / 2, VH / 2);
        recentre = false;
    }

    private static ItemStack partIcon(ToolTableShaping.Option o) {
        return new ItemStack(BuiltInRegistries.ITEM.get(o.shaping().item()));
    }

    private void stockNodes(List<BenchFan.Node> out) {
        var level = level();
        Optional<Material.Located> stuff = menu.stuff(level);
        if (stuff.isPresent() && !menu.cutTab()) {
            int have = menu.centreStack().getCount() * FormItems.volume(stuff.get().form());
            List<BenchFan.Node> instant = new ArrayList<>(), forged = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) {
                ToolTableShaping.Option o = options.get(i);
                if (!ToolTableShaping.takes(menu.station(), o, stuff.get()))
                    continue;
                BenchFan.Node n = new BenchFan.Node(BenchFan.Kind.SHAPE, partIcon(o), o.label(), "", ToolTableMenu.BTN_PICK + i, o.forged() ? 1 : 0, i);
                n.faint = have < o.cells() * io.github.soundgoodizerfan.feedback.modular.ModularTuning.CELL_MB;
                n.forged = o.forged();
                (o.forged() ? forged : instant).add(n);
            }
            out.addAll(hubbed(instant, 0));
            out.addAll(hubbed(forged, 1));
        }
        List<ItemStack> cuts = menu.cuts(level);
        List<BenchFan.Node> cutNodes = new ArrayList<>();
        for (int k = 0; k < cuts.size(); k++)
            cutNodes.add(new BenchFan.Node(BenchFan.Kind.CUT, cuts.get(k), cuts.get(k).getHoverName(), "", ToolTableMenu.BTN_CUT + k, 2, k));
        if (cutNodes.size() > HUB_AT) {
            BenchFan.Node hub = new BenchFan.Node(BenchFan.Kind.HUB, cuts.get(0), Component.translatable("gui.feedback.bench.cuts"), "", 0, 2, -1);
            hub.kids = cutNodes;
            out.add(hub);
        } else
            out.addAll(cutNodes);
        List<ItemStack> broken = menu.breakdown(level);
        if (!broken.isEmpty())
            out.add(new BenchFan.Node(BenchFan.Kind.BREAK, broken.get(0).copyWithCount(1), Component.translatable("gui.feedback.bench.break"), "", ToolTableMenu.BTN_BREAK, 3, 0));
        if (st.dishes())
            prepNodes(out, false);
    }

    /** Fold a long group into hubs by part (a part with one shape stays a node); short groups stay flat. */
    private static List<BenchFan.Node> hubbed(List<BenchFan.Node> nodes, int group) {
        if (nodes.size() <= HUB_AT)
            return nodes;
        Map<ResourceLocation, List<BenchFan.Node>> byPart = new LinkedHashMap<>();
        List<BenchFan.Node> out = new ArrayList<>();
        for (BenchFan.Node n : nodes)
            byPart.computeIfAbsent(BuiltInRegistries.ITEM.getKey(n.icon.getItem()), k -> new ArrayList<>()).add(n);
        for (List<BenchFan.Node> members : byPart.values()) {
            if (members.size() == 1) {
                out.add(members.get(0));
                continue;
            }
            BenchFan.Node hub = new BenchFan.Node(BenchFan.Kind.HUB, members.get(0).icon, Component.translatable(members.get(0).icon.getDescriptionId()), "", 0, group, -1);
            hub.kids = members;
            hub.forged = members.get(0).forged;
            out.add(hub);
        }
        return out;
    }

    private void partNodes(List<BenchFan.Node> out) {
        var level = level();
        var materials = ClientMaterials.on(level);
        for (ToolTableMenu.Add a : menu.adds(level)) {
            switch (a.source()) {
                case FITTED -> out.add(new BenchFan.Node(BenchFan.Kind.FITTED, a.icon(), a.icon().getHoverName(), "", ToolTableMenu.BTN_OFF + a.index(), 0, a.index()));
                case HELD -> out.add(new BenchFan.Node(BenchFan.Kind.HELD, a.icon().copyWithCount(1), a.icon().getHoverName(), "", ToolTableMenu.BTN_FIT + a.index(), 1, a.index()));
                case SHAPE -> {
                    ToolTableShaping.Option o = options.get(a.index());
                    BenchFan.Node n = new BenchFan.Node(BenchFan.Kind.BUILD, a.icon(), o.label(), "", ToolTableMenu.BTN_SHAPE + a.index(), 2, a.index());
                    n.faint = ToolTableShaping.check(menu.station(), o, menu.side(), materials, level).isEmpty();
                    out.add(n);
                }
            }
        }
    }

    private void toolNodes(List<BenchFan.Node> out) {
        var level = level();
        var materials = ClientMaterials.on(level);
        ItemStack tool = menu.centreStack();
        var design = ModularStacks.design(tool);
        ModularType type = design == null ? null : menu.types(level).get(design.type());
        if (type == null)
            return;
        float wear = tool.getMaxDamage() > 0 ? tool.getDamageValue() / (float) tool.getMaxDamage() : 0f;
        for (int k = 0; k < type.order().size(); k++) {
            String key = type.order().get(k);
            ModularType.Role role = type.roles().get(key);
            var part = design.part(key);
            if (part == null && !type.extras().contains(key))
                continue;
            Component name = Component.translatable("modular.feedback.role." + role.name());
            ItemStack icon = ItemStack.EMPTY;
            if (part != null) {
                List<ItemStack> items = ToolDesigns.items(part, role, materials, part.count());
                icon = items.isEmpty() ? ItemStack.EMPTY : items.get(0).copyWithCount(1);
            }
            BenchFan.Node n = new BenchFan.Node(BenchFan.Kind.ROLE, icon, name, part == null ? "+" : icon.isEmpty() ? name.getString().substring(0, 1) : "", 0, 0, k);
            n.faint = part == null;
            n.selected = k == selectedRole;
            if (part != null && key.equals(type.wearRole()))
                n.wear = wear;
            out.add(n);
        }
        if (selectedRole >= 0) {
            if (design.part(type.order().get(selectedRole)) != null)
                out.add(new BenchFan.Node(BenchFan.Kind.OFF, ItemStack.EMPTY, Component.translatable("gui.feedback.bench.off"), "x", ToolTableMenu.BTN_OFF + selectedRole, 1, selectedRole));
            for (int s : menu.roleChoices(level, selectedRole)) {
                ItemStack held = menu.getSlot(s).getItem();
                out.add(new BenchFan.Node(BenchFan.Kind.ALT, held.copyWithCount(1), held.getHoverName(), "", ToolTableMenu.BTN_SWAP + selectedRole * 256 + s, 1, s));
            }
        }
        if (st.dishes() || st.gun()) {
            if (st.dishes())
                prepNodes(out, true);
            if (st.gun() && menu.gunTable()) {
                List<Component> lines = new ArrayList<>();
                PartPresenter.sheet(lines, menu, level, tool, materials);
                sheet = lines;
            }
        }
    }

    /** The Prep Table's own work as nodes: cut and knead a food, finish a dish. */
    private void prepNodes(List<BenchFan.Node> out, boolean dish) {
        var level = level();
        if (dish) {
            BenchFan.Node f = new BenchFan.Node(BenchFan.Kind.PREP, ItemStack.EMPTY, Component.translatable("gui.feedback.prep_table.finish"), "~", 0, 2, 1);
            f.faint = !menu.preparable(level, 1);
            out.add(f);
            return;
        }
        BenchFan.Node cut = new BenchFan.Node(BenchFan.Kind.PREP, ItemStack.EMPTY, Component.translatable("gui.feedback.prep_table.cut"), "/", 0, 2, 0);
        cut.faint = !menu.preparable(level, 0);
        out.add(cut);
        BenchFan.Node knead = new BenchFan.Node(BenchFan.Kind.PREP, ItemStack.EMPTY, Component.translatable("gui.feedback.prep_table.knead"), "o", 0, 2, 2);
        knead.faint = !menu.preparable(level, 2);
        out.add(knead);
    }

    // --- acting on a node --------------------------------------------------------------------------

    private void reject(Component why) {
        skin.cue(GuiSkin.Cue.REJECT, stockKind(), 1f);
        shakeAt = GuiStyle.time();
        said = why;
        saidAt = GuiStyle.time();
    }

    private void arc(ItemStack icon) {
        arcIcon = icon.copyWithCount(1);
        arcAt = GuiStyle.time();
    }

    private void act(BenchFan.Node n) {
        var level = level();
        if (n.kind == BenchFan.Kind.HUB) {
            GuiSkin.play(skin.click(), 1f);
            return;
        }
        if (n.kind == BenchFan.Kind.ROLE) {
            selectedRole = selectedRole == n.index ? -1 : n.index;
            skin.cue(GuiSkin.Cue.STAGE, stockKind(), 1f);
            readKey = Integer.MIN_VALUE;
            return;
        }
        if (n.kind == BenchFan.Kind.PREP) {
            prepare(n);
            return;
        }
        if (!menu.hasHammer()) {
            reject(Component.translatable(st.saws() ? "gui.feedback.tool_table.status.no_axe" : st.dishes() ? "gui.feedback.prep_table.status.no_hammer"
                    : "gui.feedback.tool_table.status.no_hammer"));
            return;
        }
        if (n.faint) {
            reject(n.kind == BenchFan.Kind.BUILD ? Component.translatable("gui.feedback.bench.stock_slot")
                    : Component.translatable("gui.feedback.bench.short", ((ToolTableShaping.Option) options.get(n.index)).cells()));
            return;
        }
        switch (n.kind) {
            case SHAPE -> shape(n);
            case CUT -> {
                press(n.action);
                press(ToolTableMenu.BTN_MAKE);
                arc(n.icon);
                skin.cue(GuiSkin.Cue.FINISH, stockKind(), 1f);
            }
            case BREAK -> {
                press(n.action);
                skin.cue(GuiSkin.Cue.FINISH, stockKind(), 0.9f);
            }
            case FITTED, OFF -> {
                press(n.action);
                skin.cue(GuiSkin.Cue.ABORT, stockKind(), 1f);
            }
            default -> {
                press(n.action);
                skin.cue(GuiSkin.Cue.FINISH, stockKind(), 1.05f);
                seatAt = GuiStyle.time();
            }
        }
    }

    private void shape(BenchFan.Node n) {
        var level = level();
        ToolTableShaping.Option o = options.get(n.index);
        var stock = ToolTableShaping.check(menu.station(), o, menu.stock(), ClientMaterials.on(level), level);
        if (stock.isEmpty()) {
            reject(Component.translatable("gui.feedback.tool_table.status.cold"));
            return;
        }
        if (o.forged()) {
            if (forge == null || !forge.start(o, n.index))
                reject(Component.translatable("gui.feedback.tool_table.status.cold"));
            else
                skin.cue(GuiSkin.Cue.START, stockKind(), 1f);
            return;
        }
        press(n.action);
        press(ToolTableMenu.BTN_MAKE);
        arc(n.icon);
        skin.cue(GuiSkin.Cue.FINISH, stockKind(), 1f);
    }

    /** Open the cut (0), the pour (1) or the knead (2) minigame over the bench. */
    private void prepare(BenchFan.Node n) {
        var level = level();
        if (!settled() || n.faint || !menu.preparable(level, n.index)) {
            reject(Component.translatable(n.index == 0 && !menu.hasHammer() ? "gui.feedback.prep_table.status.no_knife" : "gui.feedback.prep_table.status.bad_food"));
            return;
        }
        skin.cue(GuiSkin.Cue.START, null, 1f);
        if (n.index == 0)
            PrepCutScreen.open(this, menu);
        else if (n.index == 2)
            PrepKneadScreen.open(this, menu);
        else
            PrepPourScreen.open(this, menu);
    }

    // --- input ---------------------------------------------------------------------------------------

    private boolean inCanvas(double mx, double my) {
        return mx >= leftPos + VX && mx < leftPos + VX + VW && my >= topPos + VY && my < topPos + VY + VH;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!settled())
            return true;
        if (forge != null && forge.click(mx, my, button))
            return true;
        Slot over = hoveredSlot;
        if (inCanvas(mx, my) && (over == null || !over.isActive()) && !isOverWidget(mx, my) && button == 0) {
            BenchFan.Node n = fan.at(mx, my, cx(), cy());
            if (n != null)
                act(n);
            else
                panning = true;
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private boolean isOverWidget(double mx, double my) {
        for (var c : children())
            if (c instanceof AbstractWidget w && w.visible && w.isMouseOver(mx, my))
                return true;
        return false;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (panning && settled()) {
            fan.pan(dx, dy);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        panning = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (settled() && !busy() && inCanvas(mx, my)) {
            fan.zoomAbout(scrollY, mx, my, cx(), cy());
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (forge != null && settled() && forge.key(key, scan, modifiers))
            return true;
        return super.keyPressed(key, scan, modifiers);
    }

    // --- drawing -------------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        partial = partialTick;
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
        int x = leftPos + VX, y = topPos + VY;
        skin.inset(g, x, y, VW, VH);
        g.enableScissor(x + 1, y + 1, x + VW - 1, y + VH - 1);
        float heat = menu.heat(level());
        if (menu.trim())
            idle(g, x + 1, y + 1, VW - 2, VH - 2);
        else if (skin == GuiSkin.FORGE)
            BenchFire.draw(g, x + 1, y + 1, VW - 2, VH - 2, Mth.clamp(heat / 1100f, 0f, 1f));
        else
            idle(g, x + 1, y + 1, VW - 2, VH - 2);
        g.disableScissor();
        int cx = cx(), cy = cy(), sx = slotCx(), sy = slotCy();
        int shake = GuiStyle.shake(shakeAt, 2);
        // The pad the stock lies on: warm with its heat, and a ring that spreads when something is set down.
        if (heat > 60f && !menu.trim())
            for (int k = 3; k >= 1; k--)
                GuiStyle.disc(g, sx + shake, sy, 10 + k * 4, (int) (0x22 * Mth.clamp(heat / 900f, 0.15f, 1f)) << 24 | BenchFire.colour(Mth.clamp(heat / 1100f, 0f, 1f)));
        if (!menu.trim())
            GuiStyle.ring(g, sx + shake, sy, 15, 1, 0x50000000 | skin.dim() & 0xFFFFFF);
        float seatAge = GuiStyle.time() - seatAt;
        if (seatAge >= 0 && seatAge < 9 && !ClientSettings.reduceMotion())
            GuiStyle.ring(g, sx, sy, 14 + Math.round(GuiStyle.easeOut(seatAge / 9f) * 18), 2, (int) (0x90 * (1 - seatAge / 9f)) << 24 | skin.accent() & 0xFFFFFF);
        // A category glyph opens when the pointer rests on it, and stays open until another opens.
        BenchFan.Node hover = fan.at(mouseX, mouseY, cx, cy);
        if (hover != null && hover.kind == BenchFan.Kind.HUB && !hover.selected && !busy()) {
            for (BenchFan.Node n : fan.nodes())
                n.selected = false;
            hover.selected = true;
            GuiSkin.play(skin.click(), 1.3f);
        }
        fan.draw(g, skin, font, x, y, VW, VH, cx, cy, sx, sy, mouseX, mouseY, shake, busy() ? 0.2f : 1f);

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
                }
            }
            skin.slot(g, leftPos + s.x, topPos + s.y);
        }
        inventory(g, ToolTableMenu.INV_X, ToolTableMenu.INV_Y);
        ghost(g);
    }

    /** What is under the pointer in the fan, shown where it would come out: a ghost of the part and a few bars, never numbers. */
    private void ghost(GuiGraphics g) {
        BenchFan.Node n = busy() ? null : fan.over();
        if (n == null || n.icon.isEmpty() || (n.kind != BenchFan.Kind.SHAPE && n.kind != BenchFan.Kind.CUT && n.kind != BenchFan.Kind.BUILD))
            return;
        Slot out = menu.getSlot(ToolTableMenu.OUT);
        if (out.hasItem())
            return;
        int ox = leftPos + out.x, oy = topPos + out.y;
        g.renderItem(n.icon, ox, oy);
        g.pose().pushPose();
        g.pose().translate(0, 0, 250);
        g.fill(ox, oy, ox + 16, oy + 16, 0x90000000 | 0x14100C);
        g.pose().popPose();
        if (n.kind != BenchFan.Kind.SHAPE && n.kind != BenchFan.Kind.BUILD)
            return;
        var level = level();
        Optional<Material.Located> stuff = n.kind == BenchFan.Kind.SHAPE ? menu.stuff(level)
                : Material.find(ClientMaterials.on(level), menu.side().get(0));
        if (stuff.isEmpty())
            return;
        PartPresenter.glyphs(stuff.get().material(), bars);
        // The alternative: the stock on the cursor, else the stock slot, if it is another material.
        ItemStack alt = menu.getCarried().isEmpty() ? menu.side().get(0) : menu.getCarried();
        Optional<Material.Located> other = alt.isEmpty() ? Optional.empty() : Material.find(ClientMaterials.on(level), alt);
        boolean compare = other.isPresent() && !other.get().id().equals(stuff.get().id());
        if (compare)
            PartPresenter.glyphs(other.get().material(), against);
        int bx = leftPos + ToolTableMenu.OUT_X - 6, by = topPos + ToolTableMenu.OUT_Y + 24;
        int[] tint = {skin.ink(), skin.accent(), skin.dim()};
        for (int i = 0; i < 3; i++) {
            int yy = by + i * 10;
            // the icon says what the bar is (hard, tough, heavy), so no text is needed
            g.pose().pushPose();
            g.pose().translate(bx, yy - 2, 0);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.renderItem(BAR_ICONS[i], 0, 0);
            g.pose().popPose();
            skin.bar(g, bx + 11, yy, 32, 4, bars[i], -1f, 0xFF000000 | tint[i] & 0xFFFFFF);
            if (compare) {
                int mark = bx + 11 + Math.round(31 * against[i]);
                g.fill(mark, yy - 1, mark + 1, yy + 6, 0xFFFFFFFF);
            }
        }
    }

    /** Hardness, toughness, weight: what the ghost's three bars are. */
    private static final ItemStack[] BAR_ICONS = {new ItemStack(net.minecraft.world.item.Items.DIAMOND),
            new ItemStack(net.minecraft.world.item.Items.SHIELD), new ItemStack(net.minecraft.world.item.Items.ANVIL)};

    /** Each skin's quiet life behind the fan (the smithing table's is the fire): a few particles from a hash of their index, no state, no allocation. */
    private void idle(GuiGraphics g, int x, int y, int w, int h) {
        if (ClientSettings.reduceMotion())
            return;
        float t = GuiStyle.time();
        switch (skin) {
            case WOOD -> drift(g, x, y, w, h, t, 9, 0xEBD9AE, false, 5, 1, 120);
            case STONE -> drift(g, x, y, w, h, t, 8, 0x9C9890, false, 0, 1, 130);
            case BRICK -> {
                if (!menu.centreStack().isEmpty())
                    drift(g, x, y, w, h, t, 6, 0xFFFFFF, true, 2, 2, 70);
            }
            case PANEL -> {
                float breath = 0.5f + 0.5f * Mth.sin(t * 0.12f);
                int bucket = (int) (t / 3) * 0x9E3779B1;
                int a = (bucket >>> 24) < 3 ? 0x20 : 0x60 + (int) (breath * 0x70);
                g.fill(x + w - 8, y + 5, x + w - 5, y + 8, a << 24 | 0xFFB030);
            }
            default -> { }
        }
    }

    private static void drift(GuiGraphics g, int x, int y, int w, int h, float t, int n, int rgb, boolean rise, int sway, int size, int maxAlpha) {
        for (int i = 0; i < n; i++) {
            int s = (i + 1) * 0x9E3779B1;
            s ^= s >>> 15;
            int period = 50 + (s >>> 4 & 31);
            float f = ((t + (s & 0xFF)) % period) / period;
            int a = (int) (maxAlpha * Mth.sin(f * Mth.PI));
            if (a < 8)
                continue;
            int px = x + (s >>> 8 & 0xFF) * w / 256 + Math.round(Mth.sin(f * 6.28f + i) * sway);
            int py = y + Math.round((rise ? 1 - f : f) * h);
            g.fill(px, py, px + size, py + size, a << 24 | rgb);
        }
    }

    /** The slot captions and the one line of text, on a raised layer so nothing in the fan hides them. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.drawString(font, title, titleLabelX, titleLabelY, skin.ink(), false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, skin.dim(), false);
        caption(g, Component.translatable(st.toolLabel()), ToolTableMenu.HAMMER_X, ToolTableMenu.HAMMER_Y, menu.getSlot(ToolTableMenu.HAMMER).isActive());
        caption(g, Component.translatable(st.dishes() ? "gui.feedback.prep_table.stock" : "gui.feedback.tool_table.stock"), ToolTableMenu.STOCK_X, ToolTableMenu.STOCK_Y, menu.getSlot(ToolTableMenu.STOCK).isActive());
        caption(g, Component.translatable(st.dishes() ? "gui.feedback.prep_table.seasoning" : "gui.feedback.tool_table.plan"), ToolTableMenu.PLAN_X, ToolTableMenu.PLAN_Y,
                menu.getSlot(ToolTableMenu.PLAN).isActive());
        if (menu.trim()) {
            // Vanilla smithing, plainly: template, base and addition in a row, labelled; the result is the slot on the right.
            caption(g, Component.translatable("gui.feedback.bench.template"), ToolTableMenu.TRIM_X, ToolTableMenu.CENTRE_Y, true);
            caption(g, Component.translatable("gui.feedback.tool_table.smith.1"), ToolTableMenu.TRIM_X + 26, ToolTableMenu.CENTRE_Y, true);
            caption(g, Component.translatable("gui.feedback.tool_table.smith.2"), ToolTableMenu.TRIM_X + 52, ToolTableMenu.CENTRE_Y, true);
        }
        if (menu.gunTable())
            caption(g, Component.translatable("gui.feedback.gun_table.round"), ToolTableMenu.ROUND_X, ToolTableMenu.ROUND_Y, true);
        caption(g, Component.translatable("gui.feedback.tool_table.result"), ToolTableMenu.OUT_X, ToolTableMenu.OUT_Y, true);
        // The one line: what the pointer is on, or what to do next.
        Component line = line();
        int ly = VY + VH - 11;
        if (!line.getString().isEmpty())
            g.drawString(font, line, VX + VW / 2 - font.width(line) / 2, ly, fan.over() != null ? skin.ink() : skin.dim(), false);
        if (!sheet.isEmpty() && net.minecraft.client.gui.screens.Screen.hasShiftDown())
            for (int i = 0; i < sheet.size(); i++)
                g.drawString(font, sheet.get(i), VX + 6, VY + 6 + i * 10, skin.ink(), false);
        g.pose().popPose();
    }

    private void caption(GuiGraphics g, Component text, int slotX, int slotY, boolean shown) {
        if (!shown)
            return;
        g.pose().pushPose();
        g.pose().translate(slotX + 8, slotY - 8, 0);
        g.pose().scale(0.8f, 0.8f, 1f);
        g.drawString(font, text, -font.width(text) / 2, 0, skin.dim(), false);
        g.pose().popPose();
    }

    private Component line() {
        if (GuiStyle.time() - saidAt < 70)
            return said;
        BenchFan.Node n = busy() ? null : fan.over();
        if (n != null) {
            if (n.kind == BenchFan.Kind.SHAPE || n.kind == BenchFan.Kind.BUILD) {
                int cells = options.get(n.index).cells();
                return Component.empty().append(n.label).append(Component.literal("  " + cells)).append(Component.translatable("gui.feedback.bench.ingots"));
            }
            return n.label;
        }
        if (busy())
            return Component.empty();
        var level = level();
        if (st.usesTool() && !menu.trim() && !menu.hasHammer())
            return Component.translatable(st.saws() ? "gui.feedback.tool_table.status.no_axe" : st.dishes() ? "gui.feedback.prep_table.status.no_hammer"
                    : "gui.feedback.tool_table.status.no_hammer");
        return switch (menu.centre(level)) {
            case EMPTY -> Component.translatable(st.dishes() ? "gui.feedback.bench.put_dish" : "gui.feedback.bench.put");
            case STOCK -> fan.isEmpty() ? Component.translatable("gui.feedback.tool_table.list.nothing") : Component.empty();
            case PART -> fan.isEmpty() ? Component.translatable(menu.cutTab() ? "gui.feedback.bench.parts_tab" : "gui.feedback.bench.nothing_fits") : Component.empty();
            case TOOL -> fan.isEmpty() && menu.cutTab() ? Component.translatable("gui.feedback.bench.parts_tab") : Component.empty();
            case TEMPLATE -> Component.translatable("gui.feedback.tool_table.status.put_smith");
            default -> Component.empty();
        };
    }

    /** An empty slot says what it takes when the pointer rests on it; Shift on a shape says what it is. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (busy())
            return;
        BenchFan.Node n = fan.over();
        if (n != null && n.kind == BenchFan.Kind.SHAPE && net.minecraft.client.gui.screens.Screen.hasShiftDown() && inCanvas(mouseX, mouseY)) {
            List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
            for (Component c : PartPresenter.describe(options.get(n.index)))
                lines.addAll(font.split(c, 190));
            g.renderTooltip(font, lines, mouseX, mouseY);
            return;
        }
        Slot s = hoveredSlot;
        if (s != null && s.isActive() && !s.hasItem() && s.index >= ToolTableMenu.HAMMER && s.index < menu.inv()) {
            String key = s.index == ToolTableMenu.HAMMER ? st.tip("hammer") : s.index == ToolTableMenu.TOOL ? st.tip("centre") : s.index == ToolTableMenu.PLAN ? st.tip("plan")
                    : s.index == menu.inv() - 1 && menu.gunTable() ? "round" : st.tip("stock");
            g.renderTooltip(font, font.split(Component.translatable("gui.feedback.tool_table.tip." + key), 190), mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        followFan();
        partial = partialTick;
        super.render(g, mouseX, mouseY, partialTick);
        if (!settled())
            return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        if (busy())
            forge.overlay(g);
        // A made part crosses from the stock to its slot on a short arc.
        float age = GuiStyle.time() - arcAt;
        if (!arcIcon.isEmpty() && age >= 0 && age < 9 && !ClientSettings.reduceMotion()) {
            float f = GuiStyle.easeOut(age / 9f);
            float x0 = slotCx() - 8, y0 = slotCy() - 8, x1 = leftPos + ToolTableMenu.OUT_X, y1 = topPos + ToolTableMenu.OUT_Y;
            g.renderItem(arcIcon, Math.round(Mth.lerp(f, x0, x1)), Math.round(Mth.lerp(f, y0, y1) - Mth.sin(f * Mth.PI) * 26));
        }
        g.pose().popPose();
    }
}
