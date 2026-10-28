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

    private static final int LIST_X = 10, LIST_Y = 40, LIST_W = 114, ROW_H = 11, ROWS = 13;
    /** The part list sits under its search box: one row shorter, starting lower. */
    private static final int PART_Y = LIST_Y + 16, PART_ROWS = 11;
    /** Ticks of notes shown above the hit line, and the pixels each tick of them takes. */
    private static final int LOOKAHEAD = 40, PX_PER_TICK = 2;
    private static final int LANE_W = 46, TRACK_H = LOOKAHEAD * PX_PER_TICK;

    private enum Phase { NONE, PLAY, DONE }

    private AbstractWidget tabs, typePicker;
    private Button apart, forge, cutBtn, finishBtn, kneadBtn, takeBtn;
    private List<ToolTableShaping.Option> options = List.of();
    private int optionsGeneration = -1;
    /** The search box over the part list, and the indices into {@code options} it lets through. */
    private net.minecraft.client.gui.components.EditBox search;
    private List<Integer> shown = List.of();
    /** Options of each part, standard shape first; a part with several is a collapsible group. */
    private final java.util.Map<ResourceLocation, List<Integer>> groups = new java.util.LinkedHashMap<>();
    private final java.util.Set<ResourceLocation> open = new java.util.HashSet<>();
    private boolean searching;
    private int scroll;
    /** The left column's text box in the other tabs: how far it is scrolled, in lines. */
    private int boxScroll;
    /** When the row under the pointer, or the chosen one, began to be read: the marquee starts from here. */
    private long marqueeFrom;
    private int marqueeRow = -1;

    // What the contents read as, recomputed when they change rather than every frame.
    private int readKey = Integer.MIN_VALUE;
    private Component status = Component.empty();
    private List<Component> lines = List.of();
    /** In assemble: for each of the six slots, the role its part fills in the tool the parts make, or null. */
    private final String[] slotRole = new String[ToolTableAssembly.GUN_SLOTS];
    /** At the Prep Table: the role the picked dish type has for each slot, shown on empty slots as a hint. */
    private final String[] slotHint = new String[ToolTableAssembly.GUN_SLOTS];
    private boolean canForge;

    // The forging screen.
    private Phase phase = Phase.NONE;
    private ToolTableShaping.Option chosen;
    private int chosenIndex;
    private Material material;
    private int step;
    private int[] live;
    private int ticks;
    private float partial;
    private final List<ForgeRhythm.Hit> hits = new ArrayList<>();
    private ForgeRhythm.Result result;
    private int flash;
    private boolean flashHeavy;
    private String flashText = "";
    /** Sparks thrown off each blow and off a perfect part: screen-space, short-lived, drawn over the forge. */
    private record Spark(float x, float y, float vx, float vy, int life, int color) { }
    private final List<Spark> sparks = new ArrayList<>();
    private final List<Integer> sparkAge = new ArrayList<>();
    private final net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create();
    /** Ticks each lane stays lit after a blow, and the colour of the grade that lit it. */
    private final int[] laneGlow = new int[2], laneColor = new int[2];
    /** Clean blows in a row; any other blow breaks it. */
    private int streak;

    public ToolTableScreen(ToolTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, skinFor(menu));
        imageWidth = ToolTableMenu.W;
        imageHeight = ToolTableMenu.H;
        titleLabelX = 10;
        titleLabelY = 6;
        inventoryLabelX = ToolTableMenu.INV_X;
        inventoryLabelY = ToolTableMenu.INV_Y - 11;
    }

    /** Each table has its own material: stone for the Tool Table, panel for the Gun Table, brick for the Prep Table. */
    private static GuiSkin skinFor(ToolTableMenu menu) {
        return menu.gunTable() ? GuiSkin.PANEL : menu.dishTable() ? GuiSkin.BRICK : menu.station() == Station.SMITHING ? GuiSkin.FORGE : GuiSkin.STONE;
    }

    @Override
    protected void init() {
        super.init();
        String third = menu.dishTable() ? "gui.feedback.prep_table.tab.prepare" : "gui.feedback.tool_table.tab.rework";
        // A part-making bench has the Parts tab alone, and the stonecutter the cut beside it (assembly stays at the Tool Table).
        boolean bench = menu.station() != Station.TOOL, cut = menu.station() == Station.STONECUTTING, smith = menu.station() == Station.SMITHING;
        List<Component> labels = !bench ? List.of(Component.translatable("gui.feedback.tool_table.tab.part"), Component.translatable("gui.feedback.tool_table.tab.assemble"),
                        Component.translatable(third))
                : cut ? List.of(Component.translatable("gui.feedback.tool_table.tab.part"), Component.translatable("gui.feedback.tool_table.tab.cut"))
                : smith ? List.of(Component.translatable("gui.feedback.tool_table.tab.part"), Component.translatable("gui.feedback.tool_table.tab.smith"))
                : List.of(Component.translatable("gui.feedback.tool_table.tab.part"));
        tabs = addRenderableWidget(GuiStyle.tabs(skin, leftPos + 10, topPos + 18, bench ? 120 : 240, 16, labels,
                () -> menu.mode() == ToolTableMenu.MODE_CUT || menu.mode() == ToolTableMenu.MODE_SMITH ? 1 : menu.mode(),
                mode -> press(mode == 1 && bench ? (cut ? ToolTableMenu.BTN_CUT : ToolTableMenu.BTN_SMITH) : ToolTableMenu.BTN_MODE + mode)));
        typePicker = addRenderableWidget(GuiStyle.stepper(skin, leftPos + LIST_X - 2, topPos + LIST_Y - 2, LIST_W + 4, 14, "<", ">",
                () -> typeName(), d -> {
                    int n = Math.max(1, menu.dishTypes(level()).size());
                    press(ToolTableMenu.BTN_TYPE + Math.floorMod(menu.typePicked(level()) + d, n));
                }, true, null));
        typePicker.visible = false;
        apart = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.tool_table.apart"),
                b -> press(ToolTableMenu.BTN_APART), leftPos + ToolTableMenu.TOOL_X + 24, topPos + ToolTableMenu.TOOL_Y, 70, 16));
        forge = addRenderableWidget(GuiStyle.primary(skin, Component.translatable("gui.feedback.tool_table.forge"),
                b -> start(), leftPos + ToolTableMenu.TOOL_X, topPos + ToolTableMenu.TOOL_Y + 10, 70, 14,
                FSounds.UI_STAMP::get));
        cutBtn = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.prep_table.cut"),
                b -> prepare(0), leftPos + ToolTableMenu.GRID_X, topPos + ToolTableMenu.GRID_Y, 70, 16));
        finishBtn = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.prep_table.finish"),
                b -> prepare(1), leftPos + ToolTableMenu.GRID_X, topPos + ToolTableMenu.GRID_Y + 20, 70, 16));
        kneadBtn = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.prep_table.knead"),
                b -> prepare(2), leftPos + ToolTableMenu.GRID_X, topPos + ToolTableMenu.GRID_Y + 40, 70, 16));
        takeBtn = addRenderableWidget(GuiStyle.button(skin, Component.translatable("gui.feedback.prep_table.apart"),
                b -> press(ToolTableMenu.BTN_APART), leftPos + ToolTableMenu.GRID_X, topPos + ToolTableMenu.GRID_Y + 60, 70, 16));
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
        for (int i = 0; i < options.size(); i++)
            groups.computeIfAbsent(options.get(i).shaping().item(), k -> new ArrayList<>()).add(i);
        for (int i = 0; i < options.size(); i++) {
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

    /** Open the cut (0), the pour (1) or the knead (2) over this screen. */
    private void prepare(int kind) {
        if (!settled() || !menu.preparable(level(), kind))
            return;
        if (kind == 0)
            PrepCutScreen.open(this, menu);
        else if (kind == 2)
            PrepKneadScreen.open(this, menu);
        else
            PrepPourScreen.open(this, menu);
    }

    private void press(int id) {
        if (Minecraft.getInstance().gameMode != null)
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    /** The picked dish type's name, for the picker. */
    private String typeName() {
        var level = level();
        List<ResourceLocation> types = level == null ? List.of() : menu.dishTypes(level);
        return types.isEmpty() ? "" : name(types.get(menu.typePicked(level))).getString();
    }

    private net.minecraft.world.level.Level level() {
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
    private Optional<ToolTableShaping.Option> current() {
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
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = menu.types(level);
        boolean out = menu.getSlot(ToolTableMenu.OUT).hasItem();
        canForge = false;
        lines = menu.mode() == ToolTableMenu.MODE_ASSEMBLE ? assembleLines(materials, menu.assemblyTypes(level)) : List.of();
        String why;
        if (menu.mode() == ToolTableMenu.MODE_SMITH) {
            why = out ? "ready" : menu.stock().get(0).isEmpty() ? "put_smith" : "nothing_smith";
        } else if (menu.mode() == ToolTableMenu.MODE_CUT) {
            why = out ? "ready" : menu.stock().get(0).isEmpty() ? "put_cut" : "pick_cut";
        } else if (!menu.hasHammer() && !(menu.dishTable() && menu.mode() == ToolTableMenu.MODE_PREPARE)) {
            why = "no_hammer";
        } else if (out) {
            why = "ready";
        } else if (menu.mode() == ToolTableMenu.MODE_PART) {
            Optional<ToolTableShaping.Option> option = current();
            Optional<ToolTableShaping.Stock> stock = option.flatMap(o -> ToolTableShaping.check(menu.station(), o, menu.stock(), materials, level));
            canForge = stock.isPresent() && option.get().forged() && ForgeRhythm.workable(stock.get().what().material(), stock.get().coldest())
                    && ForgeRhythm.movable(option.get().variant().rhythm(), menu.strikes(), stock.get().what().material());
            boolean any = menu.stock().stream().anyMatch(s -> !s.isEmpty());
            why = option.isEmpty() ? "no_shape" : canForge ? "forge" : stock.isPresent() && option.get().forged()
                    ? (ForgeRhythm.workable(stock.get().what().material(), stock.get().coldest()) ? "unmovable" : "cold")
                    : !any ? "put_stock" : "bad_stock";
        } else if (menu.mode() == ToolTableMenu.MODE_ASSEMBLE) {
            why = menu.assembly().stream().allMatch(ItemStack::isEmpty) ? "put_parts" : "nothing";
        } else if (menu.dishTable()) {
            ItemStack work = menu.work();
            why = work.isEmpty() ? "put_food" : menu.preparable(level, 2) ? "dough" : menu.preparable(level, 0) ? "cut" : menu.preparable(level, 1) ? "finish"
                    : ToolTableMenu.isDish(work) ? (ToolTableMenu.finished(level, work) ? "has_finish" : "put_seasoning")
                    : menu.hasHammer() ? "bad_food" : "no_knife";
        } else {
            why = menu.tool().isEmpty() ? "put_tool" : "nothing_rework";
        }
        status = Component.translatable("gui.feedback." + (menu.dishTable() ? "prep_table" : menu.gunTable() && why.equals("put_tool") ? "gun_table" : "tool_table") + ".status." + why);
    }

    /** The left column in assemble: what the parts make and which role each filled, or what they could become. */
    private List<Component> assembleLines(Map<ResourceLocation, Material> materials, Map<ResourceLocation, ModularType> types) {
        List<Component> out = new ArrayList<>();
        List<ItemStack> slots = menu.assembly();
        var match = ToolTableAssembly.infer(slots, menu.plan(), materials, types);
        java.util.Arrays.fill(slotRole, null);
        java.util.Arrays.fill(slotHint, null);
        if (menu.dishTable() && !types.isEmpty()) {
            ModularType type = types.values().iterator().next();
            List<String> order = type.placed();
            for (int i = 0; i < order.size() && i < slotHint.length; i++)
                slotHint[i] = roleName(type, order.get(i));
        }
        if (match.isPresent()) {
            ModularType type = types.get(match.get().type());
            out.add(Component.translatable("gui.feedback.tool_table.making", name(match.get().type())).withStyle(net.minecraft.ChatFormatting.GOLD));
            var fig = match.get().tool().get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.DISH_FIGURES.get());
            if (fig != null && fig.overflow() > 0)
                out.add(Component.translatable("gui.feedback.prep_table.overfull").withStyle(net.minecraft.ChatFormatting.RED));
            for (int i = 0; i < slots.size(); i++)
                if (match.get().role()[i] != null) {
                    slotRole[i] = Component.translatable("modular.feedback.role." + type.roles().get(match.get().role()[i]).name()).getString();
                    out.add(Component.translatable("gui.feedback.tool_table.role",
                            Component.translatable("modular.feedback.role." + type.roles().get(match.get().role()[i]).name()),
                            slots.get(i).getHoverName(), slots.get(i).getCount()));
                }
            if (menu.gunTable())
                sheet(out, match.get().tool(), materials);
            return out;
        }
        if (menu.dishTable()) {
            // The picked type's required roles that no stack in the slots can fill, said plainly.
            ModularType type = types.values().iterator().next();
            List<String> missing = new ArrayList<>();
            for (String r : type.baseForms().get(0).keySet()) {
                ModularType.Role role = type.roles().get(r);
                boolean has = slots.stream().anyMatch(st -> !st.isEmpty() && Material.find(materials, st).filter(role::accepts).isPresent()
                        && (role.part().isEmpty() || st.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MADE_OF.get())));
                if (!has)
                    missing.add(roleName(type, r));
            }
            if (!missing.isEmpty())
                out.add(Component.translatable("gui.feedback.prep_table.needs", String.join(", ", missing)));
            return out;
        }
        List<ResourceLocation> could = ToolTableAssembly.candidates(slots, menu.plan(), materials, types);
        if (could.isEmpty() || could.size() > 4)
            return out;
        for (ResourceLocation id : could) {
            ModularType type = types.get(id);
            List<String> roles = new ArrayList<>();
            for (String r : type.order())
                roles.add(Component.translatable("modular.feedback.role." + type.roles().get(r).name()).getString());
            out.add(Component.translatable("gui.feedback.tool_table.could", name(id), String.join(", ", roles)));
        }
        return out;
    }

    /**
     * The Gun Table's spec sheet: the gun the parts make, baked against the round in the round slot -- chamber against
     * rating, speed, energy, report, kick, spread and cycle, free and exact (philosophy 8, an equipment spec). The
     * table changes no figure (guns spec 13.1); it lets the player see the interaction and which side to change.
     */
    private void sheet(List<Component> out, ItemStack gun, Map<ResourceLocation, Material> materials) {
        net.minecraft.ChatFormatting dim = net.minecraft.ChatFormatting.GRAY;
        var cartridge = menu.round().get(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE.get());
        if (menu.round().isEmpty() || cartridge == null) {
            out.add(Component.translatable("screen.feedback.gun_table.add_round").withStyle(dim));
            return;
        }
        var spec = io.github.soundgoodizerfan.feedback.weapon.GunSpec.of(gun);
        var f = spec.flatMap(g -> g.bake(materials, cartridge.at(level().getGameTime())));
        if (f.isEmpty()) {
            out.add(Component.translatable("screen.feedback.gun_table.no_fit").withStyle(net.minecraft.ChatFormatting.RED));
            return;
        }
        var g = f.get();
        net.minecraft.ChatFormatting pressure = g.overpressure() > 1 ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.WHITE;
        out.add(Component.translatable("screen.feedback.gun_table.sheet").withStyle(net.minecraft.ChatFormatting.GOLD));
        out.add(Component.translatable("screen.feedback.gun_table.charge", num(g.peakPressure().value())).withStyle(pressure));
        out.add(Component.translatable("screen.feedback.gun_table.rating", num(g.chamberRating().value())));
        out.add(Component.translatable("screen.feedback.gun_table.over", num(g.overpressure())).withStyle(pressure));
        out.add(Component.translatable("screen.feedback.gun_table.speed", num(g.muzzleSpeed() * 20),
                Component.translatable(g.muzzleSpeed() * 20 >= io.github.soundgoodizerfan.feedback.core.FTuning.SOUND_MS
                        ? "screen.feedback.gun_table.supersonic" : "screen.feedback.gun_table.subsonic")));
        out.add(Component.translatable("screen.feedback.gun_table.energy", num(g.muzzleEnergy())));
        out.add(Component.translatable("screen.feedback.gun_table.report", num(g.report())));
        out.add(Component.translatable("screen.feedback.gun_table.kick", num(g.kickDeg())));
        out.add(Component.translatable("screen.feedback.gun_table.spread", num(g.spreadDeg())));
        out.add(Component.translatable("screen.feedback.gun_table.cycle", num(g.cycleMs())));
    }

    private static String num(float value) {
        return Readout.figure(io.github.soundgoodizerfan.feedback.instrument.Quantity.FIGURE, value);
    }

    private static String roleName(ModularType type, String role) {
        return Component.translatable("modular.feedback.role." + type.roles().get(role).name()).getString();
    }

    private static Component name(ResourceLocation type) {
        return BuiltInRegistries.ITEM.get(type).getDescription();
    }

    // --- the forging screen ---------------------------------------------------------------------

    private void start() {
        var level = level();
        Optional<ToolTableShaping.Option> option = current();
        if (!canForge || option.isEmpty())
            return;
        var stock = ToolTableShaping.check(option.get(), menu.stock(), ClientMaterials.on(level), level);
        if (stock.isEmpty() || !ForgeRhythm.workable(stock.get().what().material(), stock.get().coldest()))
            return;
        if (menu.machine()) {
            press(ToolTableMenu.BTN_MACHINE);
            return;
        }
        chosen = option.get();
        chosenIndex = menu.picked();
        material = stock.get().what().material();
        step = ForgeRhythm.step(material);
        live = ForgeRhythm.live(material, stock.get().coldest());
        ticks = 0;
        hits.clear();
        phase = Phase.PLAY;
        PacketDistributor.sendToServer(new ForgeBeginPayload(menu.containerId, chosenIndex));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        refreshOptions();
        read();
        boolean part = menu.mode() == ToolTableMenu.MODE_PART;
        boolean playing = phase != Phase.NONE;
        search.visible = part && !playing;
        search.setEditable(part && !playing);
        if (!search.visible)
            search.setFocused(false);
        tabs.active = !playing;
        typePicker.visible = menu.dishTable() && menu.mode() == ToolTableMenu.MODE_ASSEMBLE;
        apart.visible = menu.mode() == ToolTableMenu.MODE_REWORK && !playing && !menu.dishTable();
        apart.active = menu.hasHammer() && ModularStacks.design(menu.tool()) != null;
        forge.visible = part && !playing && current().filter(ToolTableShaping.Option::forged).isPresent();
        forge.active = canForge;
        boolean prep = menu.dishTable() && menu.mode() == ToolTableMenu.MODE_PREPARE && !playing;
        cutBtn.visible = finishBtn.visible = kneadBtn.visible = takeBtn.visible = prep;
        takeBtn.active = prep && menu.hasHammer() && ToolTableMenu.isDish(menu.work());
        kneadBtn.active = prep && menu.preparable(level(), 2);
        cutBtn.active = prep && menu.preparable(level(), 0);
        finishBtn.active = prep && menu.preparable(level(), 1);
        tickSparks();
        if (phase != Phase.PLAY)
            return;
        if (flash > 0)
            flash--;
        // The metronome: a click every step from the start, higher on the first of each four, so the
        // count-in is four clicks and the tempo is in the player's ear before a note is due.
        if (ticks % step == 0 && ticks <= ForgeRhythm.end(chosen.variant().rhythm(), step)) {
            int beat = ticks / step;
            GuiSkin.play(SoundEvents.NOTE_BLOCK_HAT.value(), beat % ForgeRhythm.COUNT_IN == 0 ? 1.6f : 1.1f);
        }
        ticks++;
        if (ticks > ForgeRhythm.end(chosen.variant().rhythm(), step) + 2 * step)
            finish();
    }

    private void finish() {
        String rhythm = chosen.variant().rhythm();
        result = ForgeRhythm.judge(rhythm, step, hits, live, ForgeRhythm.noiseFor(hits));
        if (result.perfect()) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(FSounds.FORGE_FANFARE.get(), 1f));
            float px = leftPos + 8 + (imageWidth - 16) / 2f, py = topPos + 16 + 25;
            for (int wave = 0; wave < 4; wave++)
                burst(px, py, 40, 2f + wave * 1.3f, 40, 0xFFD84A, 0xFFFFFF, 0xFFA030, 0xFFF0A0);
        }
        else if (result.flaw() >= io.github.soundgoodizerfan.feedback.modular.ForgeRhythm.MAX_FLAW)
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(FSounds.FORGE_FAILURE.get(), 1f));
        PacketDistributor.sendToServer(new ForgeFinishPayload(menu.containerId, chosenIndex,
                hits.stream().map(ForgeRhythm.Hit::tick).toList(), hits.stream().map(ForgeRhythm.Hit::heavy).toList()));
        phase = Phase.DONE;
    }

    private void strike(boolean heavy) {
        if (phase != Phase.PLAY || hits.size() >= 256)
            return;
        int tick = Math.round(ticks + partial);
        hits.add(new ForgeRhythm.Hit(tick, heavy));
        grade(tick, heavy);
        boolean dead = tick < live[0] || tick > live[1];
        GuiSkin.play(dead ? SoundEvents.ANVIL_PLACE : SoundEvents.ANVIL_LAND, heavy ? 0.7f : 1.15f);
    }

    private void burst(float x, float y, int n, float speed, int life, int... colors) {
        for (int i = 0; i < n && sparks.size() < 400; i++) {
            float a = rng.nextFloat() * Mth.TWO_PI, v = speed * (0.3f + rng.nextFloat());
            sparks.add(new Spark(x, y, Mth.cos(a) * v, Mth.sin(a) * v - speed * 0.5f, life / 2 + rng.nextInt(life / 2 + 1),
                    colors[rng.nextInt(colors.length)]));
            sparkAge.add(0);
        }
    }

    private void tickSparks() {
        for (int i = sparks.size() - 1; i >= 0; i--) {
            Spark s = sparks.get(i);
            int age = sparkAge.get(i) + 1;
            if (age >= s.life()) {
                sparks.remove(i);
                sparkAge.remove(i);
                continue;
            }
            sparks.set(i, new Spark(s.x() + s.vx(), s.y() + s.vy(), s.vx() * 0.94f, s.vy() * 0.94f + 0.25f, s.life(), s.color()));
            sparkAge.set(i, age);
        }
        laneGlow[0] = Math.max(0, laneGlow[0] - 1);
        laneGlow[1] = Math.max(0, laneGlow[1] - 1);
    }

    private void drawSparks(GuiGraphics g) {
        for (int i = 0; i < sparks.size(); i++) {
            Spark s = sparks.get(i);
            float life = 1f - (sparkAge.get(i) + partial) / s.life();
            int a = Mth.clamp((int) (life * 255), 0, 255);
            int px = Math.round(s.x() + s.vx() * partial), py = Math.round(s.y() + s.vy() * partial);
            g.fill(px, py, px + 2, py + 2, a << 24 | s.color() & 0xFFFFFF);
        }
    }

    /** What the blow just struck would score, shown on its lane for a moment: the client's guess, the server decides. */
    private void grade(int tick, boolean heavy) {
        String rhythm = chosen.variant().rhythm();
        int best = Integer.MAX_VALUE;
        for (int i = 0; i < rhythm.length(); i++) {
            char c = rhythm.charAt(i);
            if (ForgeRhythm.note(c) && (c == 'h') == heavy)
                best = Math.min(best, Math.abs(tick - ForgeRhythm.noteTick(i, step)));
        }
        flashHeavy = heavy;
        flash = 12;
        flashText = tick < live[0] || tick > live[1] ? "forge.feedback.grade.cold"
                : best <= ForgeRhythm.CLEAN_TICKS ? "forge.feedback.grade.clean"
                : best <= ForgeRhythm.LOOSE_TICKS ? "forge.feedback.grade.loose" : "forge.feedback.grade.miss";
        boolean clean = flashText.endsWith("clean"), loose = flashText.endsWith("loose");
        streak = clean ? streak + 1 : 0;
        int lane = heavy ? 1 : 0;
        int w = imageWidth - 16;
        float cx = leftPos + 8 + w / 2f + (heavy ? 4 + LANE_W / 2f : -LANE_W - 4 + LANE_W / 2f), cy = topPos + 16 + 44 + TRACK_H;
        laneGlow[lane] = 8;
        laneColor[lane] = clean ? 0x9BE08A : loose ? 0xF0D060 : 0xE06060;
        int n = (heavy ? 22 : 12) * (clean ? 2 : loose ? 1 : 0) + (clean || loose ? 0 : 5);
        if (clean)
            burst(cx, cy, n, heavy ? 3.4f : 2.6f, 22, 0xFFFFFF, 0xFFE070, 0xFFA030);
        else if (loose)
            burst(cx, cy, n, 2.2f, 16, 0xF0A040, 0xC06020);
        else
            burst(cx, cy, n, 1.2f, 12, 0x707070, 0x504040);
    }

    // --- input ----------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (phase == Phase.PLAY) {
            if (button == 0 || button == 1)
                strike(button == 1);
            return true;
        }
        if (phase == Phase.DONE) {
            phase = Phase.NONE;
            return true;
        }
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
        if (settled() && menu.mode() == ToolTableMenu.MODE_CUT && button == 0 && cutAt(mouseX, mouseY) >= 0) {
            GuiSkin.play(skin.click(), 1f);
            press(ToolTableMenu.BTN_PICK + cutAt(mouseX, mouseY));
            return true;
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
        if (phase == Phase.NONE && mouseX >= leftPos + LIST_X && mouseX < leftPos + LIST_X + LIST_W + 6
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
        if (phase != Phase.NONE && key != InputConstants.KEY_ESCAPE) {
            if (phase == Phase.PLAY && (key == InputConstants.KEY_Z || key == InputConstants.KEY_X))
                strike(key == InputConstants.KEY_X);
            else if (phase == Phase.DONE)
                phase = Phase.NONE;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    // --- drawing --------------------------------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        skin.panel(g, leftPos, topPos, imageWidth, imageHeight);
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
            skin.slot(g, leftPos + s.x, topPos + s.y);
        }
        inventory(g, ToolTableMenu.INV_X, ToolTableMenu.INV_Y);
        int hx = leftPos + ToolTableMenu.HAMMER_X, hy = topPos + ToolTableMenu.HAMMER_Y;
        Component hammer = Component.translatable(menu.dishTable() ? "gui.feedback.prep_table.knife" : "gui.feedback.tool_table.hammer"), result = Component.translatable("gui.feedback.tool_table.result");
        g.drawString(font, hammer, hx + 8 - font.width(hammer) / 2, hy - 11, skin.dim(), false);
        g.drawString(font, result, leftPos + ToolTableMenu.OUT_X + 8 - font.width(result) / 2, topPos + ToolTableMenu.OUT_Y - 12, skin.dim(), false);
        switch (menu.mode()) {
            case ToolTableMenu.MODE_PART -> list(g, mouseX, mouseY);
            case ToolTableMenu.MODE_CUT -> cutGrid(g, mouseX, mouseY);
            case ToolTableMenu.MODE_SMITH -> { }
            case ToolTableMenu.MODE_ASSEMBLE -> column(g, lines, menu.dishTable() ? 18 : 0);
            default -> column(g, helpLines(), 0);
        }
        if (menu.mode() == ToolTableMenu.MODE_ASSEMBLE)
            slotGroups(g);
        int sx = leftPos + ToolTableMenu.GRID_X;
        // Below whatever the tab puts under the slots: the plan slot, the Prep Table's two assembly rows, or its four buttons.
        int sy = topPos + (menu.mode() != ToolTableMenu.MODE_ASSEMBLE ? ToolTableMenu.GRID_Y + (menu.dishTable() ? 80 : 3 * 18 + 8)
                : menu.dishTable() ? ToolTableMenu.ASM_ROW_B + 22 : planY() + 20);
        boolean ok = menu.getSlot(ToolTableMenu.OUT).hasItem() || canForge;
        int line = 0;
        for (var l : font.split(status, ToolTableMenu.W - ToolTableMenu.GRID_X - 14)) {
            g.drawString(font, l, sx, sy + line * 10, ok ? skin.accent() : skin.dim(), false);
            line++;
        }
        if (menu.mode() == ToolTableMenu.MODE_PART)
            fit(g, selection().getString(), leftPos + ToolTableMenu.TOOL_X, topPos + ToolTableMenu.TOOL_Y + 2,
                    ToolTableMenu.W - ToolTableMenu.TOOL_X - 14, skin.ink(), false, 0);
        else if (menu.mode() == ToolTableMenu.MODE_ASSEMBLE)
        {
            if (!menu.dishTable())
                g.drawString(font, Component.translatable("gui.feedback.tool_table.plan"), leftPos + ToolTableMenu.PLAN_X + 20, topPos + planY() + 4, skin.dim(), false);
            if (menu.gunTable())
                g.drawString(font, Component.translatable("gui.feedback.gun_table.round"), leftPos + ToolTableMenu.GUN_ROUND_X + 20, topPos + planY() + 4, skin.dim(), false);
        }
        else if (menu.dishTable()) {
            // Above the slots: beside them "Work" runs into the seasoning slot.
            g.drawString(font, Component.translatable("gui.feedback.prep_table.work"), leftPos + ToolTableMenu.TOOL_X, topPos + ToolTableMenu.TOOL_Y - 9, skin.dim(), false);
            g.drawString(font, Component.translatable("gui.feedback.prep_table.seasoning"), leftPos + ToolTableMenu.SEASON_X, topPos + ToolTableMenu.SEASON_Y - 9, skin.dim(), false);
        }
        else
            g.drawString(font, Component.translatable("gui.feedback.tool_table.stock"), leftPos + ToolTableMenu.GRID_X, topPos + ToolTableMenu.GRID_Y - 11, skin.dim(), false);
    }

    /**
     * The six slots, each boxed and labelled by the role its part fills in the tool the parts make;
     * a part the parts do not use says "?". An empty slot is unlabelled: it takes anything, and the
     * left column names what the parts could still become.
     */
    /** Where the blueprint sits: the Gun Table's grid is taller. */
    private int planY() {
        return menu.gunTable() ? ToolTableMenu.GUN_PLAN_Y : ToolTableMenu.PLAN_Y;
    }

    private void slotGroups(GuiGraphics g) {
        for (int i = 0; i < (menu.gunTable() ? ToolTableAssembly.GUN_SLOTS : ToolTableAssembly.SLOTS); i++) {
            int x = leftPos + (menu.gunTable() ? ToolTableMenu.gunAsmX(i) : ToolTableMenu.asmX(i)) - 1,
                    y = topPos + (menu.gunTable() ? ToolTableMenu.gunAsmY(i) : ToolTableMenu.asmY(i)) - 1;
            g.renderOutline(x, y, 18, 18, skin.dim() & 0x60FFFFFF | 0x40000000);
            boolean filled = menu.getSlot(i < ToolTableAssembly.SLOTS ? ToolTableMenu.ASM + i : ToolTableMenu.EXTRA + i - ToolTableAssembly.SLOTS).hasItem();
            if (!filled && slotHint[i] == null)
                continue;
            String label = filled ? slotRole[i] == null ? "?" : slotRole[i] : slotHint[i];
            g.pose().pushPose();
            g.pose().translate(x + 1, y - 6, 0);
            g.pose().scale(0.5f, 0.5f, 1f);
            g.drawString(font, font.plainSubstrByWidth(label, 34), 0, 0, filled && slotRole[i] == null ? skin.warn() : skin.dim(), false);
            g.pose().popPose();
        }
    }

    /** An empty slot says what it takes when the pointer rests on it. */
    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (phase != Phase.NONE)
            return;
        if (menu.mode() == ToolTableMenu.MODE_CUT && cutAt(mouseX, mouseY) >= 0) {
            g.renderTooltip(font, menu.cuts(level()).get(cutAt(mouseX, mouseY)).value().getResultItem(level().registryAccess()), mouseX, mouseY);
            return;
        }
        if (menu.mode() == ToolTableMenu.MODE_PART) {
            int hovered = optionAt(mouseX, mouseY);
            if (hovered >= 0) {
                describe(g, options.get(hovered), mouseX, mouseY);
                return;
            }
        }
        Slot s = hoveredSlot;
        if (s != null && s.isActive() && !s.hasItem() && s.index >= ToolTableMenu.HAMMER && s.index < menu.inv()
                && !(s.index >= ToolTableMenu.STOCK && s.index < ToolTableMenu.TOOL)) {
            String key = s.index == ToolTableMenu.HAMMER ? (menu.dishTable() ? "knife" : "hammer") : s.index == ToolTableMenu.TOOL ? "tool" : s.index == ToolTableMenu.PLAN ? (menu.dishTable() ? "seasoning" : "plan")
                    : s.index == menu.inv() - 1 && menu.gunTable() ? "round" : "part";
            if (menu.gunTable() && !key.equals("round") && !key.equals("hammer"))
                key += "_gun";
            if (menu.dishTable() && key.equals("part"))
                key += "_dish";
            g.renderTooltip(font, font.split(Component.translatable("gui.feedback.tool_table.tip." + key, ToolTableAssembly.CAPACITY), 190), mouseX, mouseY);
            return;
        }
        super.renderTooltip(g, mouseX, mouseY);
    }

    /** What the chosen shape asks for, over the stock cells. */
    private Component selection() {
        return current().<Component>map(o -> Component.translatable(menu.dishTable() ? "gui.feedback.prep_table.takes" : "gui.feedback.tool_table.takes", o.cells()))
                .orElse(Component.empty());
    }

    private List<Component> helpLines() {
        if (menu.dishTable()) {
            ItemStack work = menu.work();
            List<Component> out = new ArrayList<>();
            var cut = work.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.FOOD_CUT.get());
            if (ToolTableMenu.isDish(work)) {
                // A dish's finish is a part of its design; the pour it was given is on that part.
                var d = io.github.soundgoodizerfan.feedback.modular.ModularStacks.design(work);
                var type = io.github.soundgoodizerfan.feedback.modular.ModularTypes.known().get(d.type());
                var finish = type == null || type.afterRole() == null ? null : d.part(type.afterRole());
                out.add(work.getHoverName());
                out.add(finish == null ? Component.translatable("prep.feedback.finish.none")
                        : ToolTableMenu.pourName(finish.dim("pour", io.github.soundgoodizerfan.feedback.core.FTuning.POUR_AUTOMATION)));
            } else if (!work.isEmpty()) {
                out.add(work.getHoverName());
                out.add(cut == null ? Component.translatable("prep.feedback.cut.whole") : ToolTableMenu.cutName(cut));
                var knead = work.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.FOOD_KNEAD.get());
                if (knead != null)
                    out.add(Component.translatable("prep.feedback.knead." + knead.grade()));
            }
            out.add(Component.translatable("gui.feedback.prep_table.help.prepare"));
            return out;
        }
        String t = menu.gunTable() ? "gun_table" : "tool_table";
        return List.of(Component.translatable("gui.feedback." + t + ".help.tool"), Component.translatable("gui.feedback." + t + ".help.mend"),
                Component.translatable("gui.feedback." + t + ".help.swap"), Component.translatable("gui.feedback." + t + ".help.add"),
                Component.translatable("gui.feedback." + t + ".help.apart"));
    }

    /**
     * Text that may be longer than its panel: wrapped to the box, scrolled with the wheel, clipped
     * to it by a scissor, with a thin bar like the part list's, so nothing runs over what is below.
     */
    private void column(GuiGraphics g, List<Component> text, int below) {
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

    /**
     * What a shape is, and for a variant how it differs from the standard one (spec 4.2). Text per
     * part kind and per variant is lang, written by hand: the figures it speaks of are derived
     * from the variant's data, and each line says in words which way.
     */
    private void describe(GuiGraphics g, ToolTableShaping.Option o, int mouseX, int mouseY) {
        List<Component> lines = new ArrayList<>();
        lines.add(o.label());
        String part = "modular.feedback.desc.part." + o.shaping().item().getPath();
        if (net.minecraft.client.resources.language.I18n.exists(part))
            lines.add(Component.translatable(part).withStyle(net.minecraft.ChatFormatting.GRAY));
        String variant = "modular.feedback.desc.variant." + o.variant().name();
        if (!o.variant().name().isEmpty() && net.minecraft.client.resources.language.I18n.exists(variant))
            lines.add(Component.translatable(variant).withStyle(net.minecraft.ChatFormatting.YELLOW));
        for (io.github.soundgoodizerfan.feedback.modular.ModularModifiers.Modifier m : io.github.soundgoodizerfan.feedback.modular.ModularModifiers.forPart(o))
            if (m.kind() == io.github.soundgoodizerfan.feedback.modular.ModularModifiers.Kind.TRAIT)
                lines.add(m.line());
        List<net.minecraft.util.FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component c : lines)
            wrapped.addAll(font.split(c, 190));
        g.renderTooltip(font, wrapped, mouseX, mouseY);
    }

    /** The stonecutter's results for the input in the first cell, one icon each; the one picked is what the output slot holds. */
    private static final int CUT_COLS = 6, CUT_CELL = 18;

    private int cutAt(double mouseX, double mouseY) {
        int cx = (int) Math.floor((mouseX - leftPos - LIST_X) / CUT_CELL), cy = (int) Math.floor((mouseY - topPos - PART_Y) / CUT_CELL);
        int i = cy * CUT_COLS + cx;
        return cx >= 0 && cx < CUT_COLS && cy >= 0 && cy * CUT_CELL < PART_ROWS * ROW_H && i < menu.cuts(level()).size() ? i : -1;
    }

    private void cutGrid(GuiGraphics g, int mouseX, int mouseY) {
        int x = leftPos + LIST_X, y = topPos + PART_Y;
        skin.inset(g, x - 2, y - 2, LIST_W + 4, PART_ROWS * ROW_H + 4);
        var all = menu.cuts(level());
        int hovered = cutAt(mouseX, mouseY);
        for (int i = 0; i < all.size() && i / CUT_COLS * CUT_CELL < PART_ROWS * ROW_H; i++) {
            int cx = x + i % CUT_COLS * CUT_CELL, cy = y + i / CUT_COLS * CUT_CELL;
            skin.row(g, cx, cy, CUT_CELL, CUT_CELL, i == menu.picked(), i == hovered);
            g.renderItem(all.get(i).value().getResultItem(level().registryAccess()), cx + 1, cy + 1);
        }
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
        if (shown.isEmpty())
            g.drawString(font, Component.translatable("gui.feedback.tool_table.search.none"), x + 2, y + 2, skin.dim(), false);
        scrollbar(g, x + LIST_W + 2, y, PART_ROWS * ROW_H, shown.size(), PART_ROWS, scroll);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        partial = partialTick;
        super.render(g, mouseX, mouseY, partialTick);
        if (phase == Phase.NONE || !settled())
            return;
        g.pose().pushPose();
        g.pose().translate(0, 0, 400);
        overlay(g);
        g.pose().popPose();
    }

    /** A glow from dull red through orange to yellow-white, by where the stock is against its window. */
    private int glow(float temperature) {
        float from = Math.max(300f, material.minTemperature().value() - 150f), to = Math.min(1500f, material.minTemperature().value() + 500f);
        float t = Math.max(0f, Math.min(1f, (temperature - from) / Math.max(1f, to - from)));
        int r = 0x40 + (int) (0xBF * t), gr = (int) (0x90 * t * t), b = (int) (0x50 * t * t * t);
        return 0xFF000000 | r << 16 | gr << 8 | b;
    }

    private void overlay(GuiGraphics g) {
        int x = leftPos + 8, y = topPos + 16, w = imageWidth - 16, h = 160;
        g.fill(x, y, x + w, y + h, 0xF01A1715);
        skin.inset(g, x, y, w, h);
        Component t = Component.literal(chosen.label().getString().toUpperCase(Locale.ROOT));
        g.drawString(font, t, x + (w - font.width(t)) / 2, y + 4, skin.ink(), false);
        int cx = x + w / 2;
        g.fill(cx - 12, y + 14, cx + 12, y + 36, glow(menu.heat(level())));
        g.renderItem(partStack(), cx - 8, y + 17);
        if (phase == Phase.PLAY)
            track(g, x, y, w);
        else
            summary(g, x, y, w);
        drawSparks(g);
    }

    /** The part this shape makes, for the picture and for the message: one of its own stock, as a stand-in. */
    private ItemStack partStack() {
        return BuiltInRegistries.ITEM.getOptional(chosen.shaping().item()).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** A filled disc, drawn a scanline at a time. */
    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    /** A ring: a disc with the middle taken out. */
    private static void ring(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int outer = (int) Math.round(Math.sqrt(r * r - dy * dy + 0.25));
            int inner = Math.abs(dy) >= r - 1 ? -1 : (int) Math.round(Math.sqrt((r - 2) * (r - 2) - dy * dy + 0.25));
            if (inner < 0) {
                g.fill(cx - outer, cy + dy, cx + outer + 1, cy + dy + 1, color);
            } else {
                g.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, color);
                g.fill(cx + inner + 1, cy + dy, cx + outer + 1, cy + dy + 1, color);
            }
        }
    }

    /** A note: a rimmed disc with a highlight, a small pale one for a tap and a big warm one for a heavy blow. */
    private static void note(GuiGraphics g, int cx, int cy, boolean heavy, boolean dead) {
        int r = heavy ? 10 : 7;
        int rim = dead ? 0xFF4A4A4A : heavy ? 0xFF8A4A10 : 0xFF6C7480;
        int face = dead ? 0xFF6E6E6E : heavy ? 0xFFF0A848 : 0xFFE6ECF2;
        int shine = dead ? 0xFF8A8A8A : heavy ? 0xFFFFD890 : 0xFFFFFFFF;
        disc(g, cx, cy, r, rim);
        disc(g, cx, cy, r - 2, face);
        disc(g, cx - r / 3, cy - r / 3, Math.max(1, r / 4), shine);
    }

    private void track(GuiGraphics g, int x, int y, int w) {
        String rhythm = chosen.variant().rhythm();
        int tapX = x + w / 2 - LANE_W - 4, heavyX = x + w / 2 + 4;
        int top = y + 44, line = top + TRACK_H;
        int tapC = tapX + LANE_W / 2, heavyC = heavyX + LANE_W / 2;
        g.fill(tapX, top, tapX + LANE_W, line + 12, 0x50000000);
        g.fill(heavyX, top, heavyX + LANE_W, line + 12, 0x50000000);
        float now = ticks + partial;
        // The beat, sliding down with the notes: a faint line each step, bright on the first of four.
        for (int beat = 0; beat * step <= ForgeRhythm.end(rhythm, step); beat++) {
            float ahead = beat * step - now;
            if (ahead > LOOKAHEAD || ahead < -2)
                continue;
            int by = line - Math.round(ahead * PX_PER_TICK);
            int c = beat % ForgeRhythm.COUNT_IN == 0 ? 0x50FFFFFF : 0x20FFFFFF;
            g.fill(tapX, by, heavyX + LANE_W, by + 1, c);
        }
        for (int lane = 0; lane < 2; lane++)
            if (laneGlow[lane] > 0) {
                int lx = lane == 0 ? tapX : heavyX;
                g.fillGradient(lx, top, lx + LANE_W, line + 12, 0x00000000, laneGlow[lane] * 12 << 24 | laneColor[lane]);
            }
        ring(g, tapC, line, 9, skin.accent());
        ring(g, heavyC, line, 12, skin.accent());
        g.drawString(font, Component.translatable("screen.feedback.forge.tap"), tapX + 4, line + 16, skin.dim(), false);
        g.drawString(font, Component.translatable("screen.feedback.forge.heavy"), heavyX + 4, line + 16, skin.dim(), false);
        for (int i = 0; i < rhythm.length(); i++) {
            char c = rhythm.charAt(i);
            if (!ForgeRhythm.note(c))
                continue;
            int due = ForgeRhythm.noteTick(i, step);
            float ahead = due - now;
            if (ahead > LOOKAHEAD || ahead < -8)
                continue;
            int ny = line - Math.round(ahead * PX_PER_TICK);
            note(g, c == 'h' ? heavyC : tapC, ny, c == 'h', due < live[0] || due > live[1]);
        }
        if (now < ForgeRhythm.COUNT_IN * step) {
            Component count = Component.literal(String.valueOf(ForgeRhythm.COUNT_IN - (int) (now / step)));
            g.drawString(font, count, x + (w - font.width(count)) / 2, top - 8, skin.accent(), false);
        }
        if (streak >= 3) {
            Component combo = Component.literal(streak + "x");
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, top + 6, 0);
            float pop = 1.4f + Math.min(streak, 20) * 0.03f;
            g.pose().scale(pop, pop, 1f);
            g.drawString(font, combo, -font.width(combo) / 2, 0, streak >= 10 ? 0xFFFFD84A : 0xFF9BE08A, true);
            g.pose().popPose();
        }
        if (flash > 0) {
            Component text = Component.translatable(flashText);
            int fc = flashText.endsWith("clean") ? 0xFF9BE08A : flashText.endsWith("loose") ? 0xFFF0D060 : 0xFFE06060;
            g.drawString(font, text, (flashHeavy ? heavyC : tapC) - font.width(text) / 2, line - 24, fc, false);
        }
    }

    private void summary(GuiGraphics g, int x, int y, int w) {
        int ly = y + 46;
        Component head = result.perfect() ? Component.translatable("forge.feedback.perfect")
                : Component.translatable("forge.feedback.result." + result.flaw(), chosen.label());
        for (var l : font.split(head, w - 24)) {
            g.drawString(font, l, x + (w - font.width(l)) / 2, ly, result.perfect() ? 0xFFFFD84A : result.flaw() == 0 ? skin.accent() : skin.warn(), false);
            ly += 10;
        }
        ly += 6;
        String[] rows = {"clean", "loose", "missed", "dead", "stray"};
        int[] counts = {result.clean(), result.loose(), result.missed(), result.dead(), result.stray()};
        for (int i = 0; i < rows.length; i++) {
            g.drawString(font, Component.translatable("screen.feedback.forge." + rows[i], counts[i]), x + 40, ly, skin.ink(), false);
            ly += 11;
        }
        Component more = Component.translatable("gui.feedback.tool_table.continue");
        g.drawString(font, more, x + (w - font.width(more)) / 2, y + 140, skin.dim(), false);
    }
}
