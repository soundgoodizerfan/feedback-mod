// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.machine.hammer.MechanicalHammerBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlockEntity;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodCut;
import io.github.soundgoodizerfan.feedback.process.FoodKnead;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FormItems;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The bench's menu. The player never picks a mode: what is in the centre slot says what the bench is doing
 * ({@code docs/gui/gui_bench.md}). The mode below is derived from it on the server each time the contents change. The benches make
 * parts only; assembly, repair, part swap, attachment fitting and take-apart are the Engineering Table's.
 *
 * <ul>
 *   <li><b>Stock</b> in the centre ({@link Centre#STOCK}) -- the shapes it can become ({@link PartBenchShaping}), the
 *       cuts the vanilla stonecutter or the saw make of it, and "break down" into smaller stock. A shape spends only
 *       the volume it needs and the rest comes back as smaller items ({@link PartBenchShaping#change}). A forged shape
 *       is played in the screen ({@link #beginForge}, {@link #finishForge}).</li>
 *   <li>At the Prep Table a <b>container</b> in the centre ({@link Centre#PART}) -- everything that can be added to it
 *       ({@link #adds}); a node fits a food from the inventory ({@link #BTN_FIT}) or shapes one from the stock slot
 *       ({@link #BTN_SHAPE}). Fitted foods sit in the assembly slots and come off again ({@link #BTN_OFF}); the moment they
 *       make a dish it is the centre ({@link ModularAssembly}).</li>
 *   <li>At the Prep Table a <b>dish</b> in the centre ({@link Centre#TOOL}) -- its finish is poured on.</li>
 * </ul>
 *
 * <p>The output is computed from the contents on the server and sent to the client the way a
 * crafting grid does; taking it spends what made it, and a point of the hammer's durability
 * (hand-tool wear, the accepted exception). No hammer in the hammer slot, no work.
 */
public class PartBenchMenu extends AbstractContainerMenu {

    /** Menu slot numbers; the table's own container is {@link PartBenchBlockEntity}'s numbering. */
    public static final int OUT = 0, HAMMER = 1, ASM = 2, STOCK = ASM + ModularAssembly.DISH_SLOTS,
            TOOL = STOCK + PartBenchBlockEntity.STOCK_SLOTS, PLAN = TOOL + 1, EXTRA = PLAN + 1;
    /** What the bench is doing, set from the centre: stock shapes, a dish being assembled, a dish being finished ({@link #MODE_PREPARE}, both at the Prep Table), a smithing template. */
    public static final int MODE_PART = 0, MODE_ASSEMBLE = 1, MODE_PREPARE = 2, MODE_SMITH = 4;
    /** What the centre slot holds; {@code PART} and {@code TOOL} are a dish's container and the dish itself, at the Prep Table only. */
    public enum Centre { EMPTY, STOCK, PART, TOOL, TEMPLATE, BLOOM, FILE }
    /**
     * Button ids. {@code PICK + n} chooses shape n, {@code CUT + n} cut n, {@code FIT + s} fits what lies in menu slot s (at the Prep Table),
     * {@code SHAPE + n} shapes option n from the stock slot and fits it, {@code OFF + k} takes off fitted food k,
     * {@code TYPE + n} chooses dish type n.
     */
    public static final int BTN_MACHINE = 4, BTN_BREAK = 5, BTN_MAKE = 6, BTN_DESELECT = 7, BTN_TAB = 8, BTN_TYPE = 50, BTN_PICK = 100, BTN_CUT = 10000,
            BTN_FIT = 20000, BTN_SHAPE = 30000, BTN_OFF = 40000, BTN_AXIS = 58000, BTN_AXIS_SET = 60000, BTN_FILE = 90000;
    /**
     * Aim buttons for the picked shape's part axes ({@link PartBenchShaping#aimable}, at most {@link #MAX_AIM}):
     * {@code AXIS + 2a} lowers axis a by its grain, {@code AXIS + 2a + 1} raises it; {@code AXIS_SET + 1000a + n} sets it to
     * its minimum plus n grains (a drag). Clamped to the axis's range on the server.
     */
    public static final int MAX_AIM = 8;

    /** The bench: wide, a fan above and the inventory below; the tool slots in a column on the right. */
    public static final int W = 360, H = 272, INV_X = 99, INV_Y = 180;
    /** The slot the work sits in, at the middle of the fan's canvas (item position). */
    public static final int CENTRE_X = 152, CENTRE_Y = 86;
    public static final int HAMMER_X = 326, HAMMER_Y = 20, STOCK_X = 326, STOCK_Y = 50, STOCK1_Y = 70, PLAN_X = 326, PLAN_Y = 96,
            OUT_X = 326, OUT_Y = 152, TRIM_X = 108, TRIM_GAP = 44;
    /** The Blueprint tab's paper slot and the height its board gets: the board fills the bench above the hotbar's row. */
    public static final int PAPER_X = 294, PAPER_Y = INV_Y + 72, BOARD_H = INV_Y + 68;

    /** Most stock slots a composed part takes beside the centre's (a bedroll's three layers want two). */
    public static final int MAX_EXTRA_SLOTS = 2;

    /** Whether the stock slot for slot {@code i + 1} of the picked part is on show: not on the other tabs, and only while the picked shape has that many slots. */
    private java.util.function.BooleanSupplier extraShown(int i) {
        return () -> !trim() && !cutTab() && !blueprintTab() && extraSlots(player.level()) > i;
    }

    private final Container table;
    private final SimpleContainer output = new SimpleContainer(1);
    /** The Prep Table's Blueprint tab's paper; empty and unused elsewhere. */
    private final SimpleContainer paper = new SimpleContainer(1);
    private final BlockPos pos;
    private final Player player;
    private final DataSlot mode = DataSlot.standalone();
    private final DataSlot pick = DataSlot.standalone();
    /** At the Prep Table: which dish type fitting builds, an index into {@link #dishTypes}. */
    private final DataSlot dishType = DataSlot.standalone();
    /** The open folder tab ({@link Station#tabs}); server-chosen by {@link #BTN_TAB} buttons. */
    private final MenuTabs tabs;
    /** The heat a Hearth beside the table holds the stock at, in Tu (0 for none): synced so the screen's heat clock agrees with the server's. */
    private final DataSlot holdTu = DataSlot.standalone();
    /** The aimed value of each axis of the picked shape's part, times ten: server-set, synced for the screen's strip and preview. */
    private final DataSlot[] aim = new DataSlot[MAX_AIM];
    /** Server side: the shape the aim was last reset for, so a new pick starts every axis at its default. */
    private int aimFor = -1;
    /** Which table this is: the part benches' menu with a type filter, at the Prep Table the dish types and a knife for the hammer. */
    private final ModularAssembly.Kind kind;
    private final Station station;
    private final boolean dish;
    /** The shape being forged, or -1: set when the player begins a pattern, so a finish names the same one. */
    private int forging = -1;
    /** The Prepare tab's minigame in play: which (0 cut, 1 pour, 2 knead, -1 none), the food it began on, and the tick it began. */
    private int prepKind = -1;
    private net.minecraft.world.item.Item prepItem = net.minecraft.world.item.Items.AIR, prepSeason = net.minecraft.world.item.Items.AIR;
    private ModularDesign prepDesign;
    private long prepStart;
    /** Hash of the table's contents when the output was last made, so a change by another viewer is noticed. */
    private int seen;
    /** The centre's item when the choice was made, so a new thing in the centre clears it. */
    private net.minecraft.world.item.Item centreWas = net.minecraft.world.item.Items.AIR;

    /** Any of the benches: the Prep Table's and the part-making stations' menus are this class with their {@code kind}. */
    protected PartBenchMenu(net.minecraft.world.inventory.MenuType<?> type, ModularAssembly.Kind kind, int id, Inventory inventory, Container table, BlockPos pos) {
        this(type, kind, id, inventory, table, pos, Station.TOOL);
    }

    /** A part-making bench ({@link StationMenu}): the Tool Table's menu with {@code station} filtering the stock. */
    protected PartBenchMenu(net.minecraft.world.inventory.MenuType<?> type, ModularAssembly.Kind kind, int id, Inventory inventory, Container table, BlockPos pos,
                            Station station) {
        super(type, id);
        this.station = station;
        this.kind = kind;
        this.dish = kind == ModularAssembly.Kind.DISH;
        this.tabs = new MenuTabs(BTN_TAB, tabList().size());
        pick.set(-1);
        this.player = inventory.player;
        this.pos = pos;
        this.table = new Watched(table, () -> {
            if (!player.level().isClientSide)
                refresh(true);
        });
        addSlot(new Slot(output, 0, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return !blueprintTab();
            }

            @Override
            public void onTake(Player p, ItemStack stack) {
                taken();
            }
        });
        addSlot(new TableSlot(PartBenchBlockEntity.HAMMER, HAMMER_X, HAMMER_Y, () -> station.usesTool() && !trim() && !blueprintTab(), 1, this::workTool));
        // The fitted foods of a dish: nodes on the fan, not slots on the screen.
        for (int i = 0; i < ModularAssembly.DISH_SLOTS; i++)
            addSlot(new TableSlot(PartBenchBlockEntity.ASSEMBLY + i, -1000, -1000, () -> false, 1, s -> true));
        for (int i = 0; i < PartBenchBlockEntity.STOCK_SLOTS; i++)
            addSlot(new TableSlot(PartBenchBlockEntity.STOCK + i, i < MAX_EXTRA_SLOTS ? STOCK_X : -1000, STOCK_Y + i * (STOCK1_Y - STOCK_Y),
                    i == 0 && dish ? () -> !trim() && !cutTab() && !blueprintTab()
                            : i < MAX_EXTRA_SLOTS && !dish ? extraShown(i) : () -> false, 64, s -> true));
        // The centre: stock, a part, a tool, or at the Prep Table a food or a dish.
        addSlot(new TableSlot(PartBenchBlockEntity.TOOL, CENTRE_X, CENTRE_Y, () -> !trim() && !blueprintTab(), 64, s -> true));
        // The seasoning slot, at the Prep Table only: what a finish pours onto the dish.
        addSlot(new TableSlot(PartBenchBlockEntity.PLAN, PLAN_X, PLAN_Y, () -> dish && !trim() && !blueprintTab(), 1,
                s -> dish && isSeasoning(player.level(), s)));
        if (station.smiths()) {
            // The vanilla smithing table, on the Trim tab: template, base and addition in a row (the same containers the centre and stock slot use).
            addSlot(new TableSlot(PartBenchBlockEntity.TOOL, TRIM_X, CENTRE_Y, this::trim, 64, s -> true));
            addSlot(new TableSlot(PartBenchBlockEntity.STOCK, TRIM_X + TRIM_GAP, CENTRE_Y, this::trim, 64, s -> true));
            addSlot(new TableSlot(PartBenchBlockEntity.STOCK + 1, TRIM_X + 2 * TRIM_GAP, CENTRE_Y, this::trim, 64, s -> true));
        }
        if (dish) {
            // The Blueprint tab's paper, kept in the menu (it goes back to the player when the bench closes), at the bench's lower right.
            addSlot(new Slot(paper, 0, PAPER_X, PAPER_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(io.github.soundgoodizerfan.feedback.registry.FTags.BLUEPRINT_PAPER);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public boolean isActive() {
                    return blueprintTab();
                }
            });
        }
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        addDataSlot(mode);
        addDataSlot(pick);
        addDataSlot(dishType);
        addDataSlot(tabs.slot());
        addDataSlot(holdTu);
        for (int i = 0; i < MAX_AIM; i++) {
            aim[i] = DataSlot.standalone();
            addDataSlot(aim[i]);
        }
        if (!player.level().isClientSide)
            output.setItem(0, preview(player.level()));
    }

    /** A slot of the table's own container; the fitted parts' are out of sight, their nodes are on the fan. */
    private class TableSlot extends Slot {
        private final java.util.function.BooleanSupplier shown;
        private final int cap;
        private final Predicate<ItemStack> takes;

        TableSlot(int index, int x, int y, java.util.function.BooleanSupplier shown, int cap, Predicate<ItemStack> takes) {
            super(table, index, x, y);
            this.shown = shown;
            this.cap = cap;
            this.takes = takes;
        }

        @Override
        public boolean isActive() {
            return shown.getAsBoolean();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return shown.getAsBoolean() && takes.test(stack);
        }

        @Override
        public int getMaxStackSize() {
            return cap;
        }
    }

    /** The table's container, telling the menu when anything in it changes: what a crafting grid does for its menu. */
    public static final class Watched implements Container {
        private final Container inner;
        private final Runnable changed;

        public Watched(Container inner, Runnable changed) {
            this.inner = inner;
            this.changed = changed;
        }

        @Override
        public int getContainerSize() {
            return inner.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return inner.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return inner.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack out = inner.removeItem(slot, amount);
            changed.run();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return inner.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            inner.setItem(slot, stack);
            changed.run();
        }

        @Override
        public void setChanged() {
            inner.setChanged();
            changed.run();
        }

        @Override
        public boolean stillValid(Player player) {
            return inner.stillValid(player);
        }

        @Override
        public void clearContent() {
            inner.clearContent();
        }

        @Override
        public int getMaxStackSize() {
            return inner.getMaxStackSize();
        }
    }

    public BlockPos getPos() {
        return pos;
    }

    /** What the bench is doing, derived from the centre on the server and synced: a {@code MODE_} constant. */
    public int mode() {
        return mode.get();
    }

    /** Which shape of the list is chosen, as an index into {@link PartBenchShaping#options}, or -1 (also while a cut is chosen). */
    public int picked() {
        return pick.get() >= BTN_CUT ? -1 : pick.get();
    }

    /** Which cut is chosen (an index into {@link #cuts}), or -1. */
    public int cutPicked() {
        return pick.get() >= BTN_CUT ? pick.get() - BTN_CUT : -1;
    }

    public ItemStack hammer() {
        return table.getItem(PartBenchBlockEntity.HAMMER);
    }

    /** The centre slot's stack. */
    public ItemStack centreStack() {
        return table.getItem(PartBenchBlockEntity.TOOL);
    }

    /** The finished dish in the centre, else empty. */
    public ItemStack tool() {
        return mode.get() == MODE_PREPARE ? centreStack() : ItemStack.EMPTY;
    }

    /** The fitted foods: the assembly slots, empty ones included. */
    public List<ItemStack> assembly() {
        return range(PartBenchBlockEntity.ASSEMBLY, ModularAssembly.DISH_SLOTS);
    }

    /** The container index of fitted food {@code i}. */
    private static int asmIndex(int i) {
        return PartBenchBlockEntity.ASSEMBLY + i;
    }

    /** What is put together at the centre: the container there and every food fitted to it. */
    public List<ItemStack> parts() {
        List<ItemStack> out = new ArrayList<>();
        out.add(centreStack());
        for (ItemStack s : assembly())
            if (!s.isEmpty())
                out.add(s);
        return out;
    }

    /** The menu slot index where the player's inventory begins. */
    public int inv() {
        return EXTRA + (dish ? 1 : 0) + (station.smiths() ? 3 : 0);
    }

    /** Which bench this is: {@link Station#TOOL} at the Prep Table (its menu carries the kind). */
    public Station station() {
        return station;
    }

    /** What the centre slot holds. */
    public Centre centre(Level level) {
        if (trim())
            return Centre.TEMPLATE;
        ItemStack c = centreStack();
        if (c.isEmpty())
            return Centre.EMPTY;
        // Only the Prep Table works on a container or a dish; at the part benches a part or a tool is not stock to shape and nothing is done with it.
        if (dish && ModularStacks.design(c) != null && !c.is(FModular.BLUEPRINT.get()) && ModularAssembly.kindOf(c) == kind)
            return Centre.TOOL;
        if (dish && c.getItem() instanceof ModularPartItem && c.has(FDataComponents.MADE_OF.get()))
            return Centre.PART;
        // A bloom (spongy iron with slag in it) is shingled, not shaped: the anvil plays it as its own work.
        if (station.forges() && Material.find(ClientMaterials.on(level), c.getItem()).map(l -> l.form().equals("bloom")).orElse(false))
            return Centre.BLOOM;
        // A flatbread is its own container: it opens the picker like a bowl does.
        if (dish && Material.find(ClientMaterials.on(level), c.getItem()).map(l -> l.material().traits().contains(Material.Trait.WRAPPER)).orElse(false))
            return Centre.PART;
        // A made part with axes is filed down, not shaped: one stroke of a file takes a grain off one of them.
        if (!dish && !Filing.axes(c, types(level)).isEmpty())
            return Centre.FILE;
        return Centre.STOCK;
    }

    /** The open folder tab's index. */
    public int tab() {
        return tabs.get();
    }

    private String tabId() {
        List<Station.Tab> list = tabList();
        return list.isEmpty() ? "" : list.get(tabs.get()).id();
    }

    /** The folder tabs of this menu's bench: the Prep Table's menu runs on the Tool Table's stock filter but has its own tabs. */
    private List<Station.Tab> tabList() {
        return dish ? Station.PREP.tabs() : station.tabs();
    }

    /**
     * The Prep Table's Blueprint tab: the drafting board over the whole bench, drawing dishes only, with the paper slot and the hotbar. The
     * bench's own slots are switched off while it is open.
     */
    public boolean blueprintTab() {
        return dish && tabId().equals("blueprint");
    }

    /** The smithing table's Trim tab: vanilla smithing, template in the first slot, no fan. */
    public boolean trim() {
        return tabId().equals("trim");
    }

    /** The stonecutter's Cut tab or the woodworking table's Saw tab: the vanilla cuts instead of the shapes. */
    public boolean cutTab() {
        return tabId().equals("cut") || tabId().equals("saw");
    }

    /** A cut is the chosen thing: vanilla's stonecutter list or the saw's, for the stock in the centre. */
    private boolean cutting() {
        return mode.get() == MODE_PART && pick.get() >= BTN_CUT;
    }

    private boolean smithing() {
        return mode.get() == MODE_SMITH;
    }

    private net.minecraft.world.item.crafting.SmithingRecipeInput smithInput() {
        return new net.minecraft.world.item.crafting.SmithingRecipeInput(centreStack(),
                table.getItem(PartBenchBlockEntity.STOCK), table.getItem(PartBenchBlockEntity.STOCK + 1));
    }

    /** What vanilla's smithing recipes make of the template in the centre with the base and addition in the stock slots, or empty. */
    private ItemStack smithed(Level level) {
        var input = smithInput();
        return level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, level)
                .map(r -> r.value().assemble(input, level.registryAccess())).orElse(ItemStack.EMPTY);
    }

    /**
     * What the stock in the centre can be cut into, one stack each in list order: at the stonecutter vanilla's own list, read
     * from the recipe manager; at the woodworking table the {@link WoodCuts} of the plank or log; nowhere else.
     */
    public List<ItemStack> cuts(Level level) {
        ItemStack in = centreStack();
        if (in.isEmpty() || mode.get() != MODE_PART || !cutTab())
            return List.of();
        if (sawing(level))
            return WoodCuts.of(level, in).stream().map(c -> c.result().copy()).toList();
        return level.getRecipeManager().getRecipesFor(net.minecraft.world.item.crafting.RecipeType.STONECUTTING,
                new net.minecraft.world.item.crafting.SingleRecipeInput(in), level).stream()
                .map(r -> r.value().getResultItem(level.registryAccess()).copy()).toList();
    }

    /** Whether the Cut or Saw tab lists the saw's wood cuts: always at the woodworking table, at the stonecutter when the stock is a plank or log. */
    private boolean sawing(Level level) {
        return station == Station.WOODWORKING || station == Station.STONECUTTING && !WoodCuts.of(level, centreStack()).isEmpty();
    }

    /** The saw's pick, if the pick is on the list. */
    private Optional<WoodCuts.Cut> woodCut(Level level) {
        List<WoodCuts.Cut> all = WoodCuts.of(level, centreStack());
        int k = cutPicked();
        return k >= 0 && k < all.size() ? Optional.of(all.get(k)) : Optional.empty();
    }

    /** Whether this is the Prep Table. */
    public boolean dishTable() {
        return dish;
    }

    /** Whether {@code stack} is what the tool slot takes: a knife at the Prep Table, an axe at the woodworking table, a saw at the stonecutter, a hand hammer elsewhere. */
    public boolean workTool(ItemStack stack) {
        return !station.usesTool() ? false : dish ? PartBenchBlockEntity.isKnife(stack) : station == Station.WOODWORKING ? stack.is(net.minecraft.tags.ItemTags.AXES) || stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.AXE_DIG)
                : station == Station.STONECUTTING ? PartBenchBlockEntity.isSaw(stack) : PartBenchBlockEntity.isHammer(stack);
    }

    /** The types this table's shapes come from: dishes at the Prep Table, every other type (guns included) at the part benches. */
    public Map<ResourceLocation, ModularType> types(Level level) {
        return ModularAssembly.ofKind(ModularTypes.on(level), kind);
    }

    /** The types whose parts the shapes list makes: this table's own (the dish's containers are shaped at the Prep Table). */
    public Map<ResourceLocation, ModularType> shapes(Level level) {
        return types(level);
    }

    /** The dish types the Prep Table's picker offers, in the picker's order. */
    public List<ResourceLocation> dishTypes(Level level) {
        return new ArrayList<>(types(level).keySet());
    }

    /** Which dish type is picked, kept inside the list. */
    public int typePicked(Level level) {
        return Math.max(0, Math.min(dishType.get(), dishTypes(level).size() - 1));
    }

    /** What fitting infers from: the picked dish type alone at the Prep Table, else every type of the table. */
    public Map<ResourceLocation, ModularType> assemblyTypes(Level level) {
        Map<ResourceLocation, ModularType> all = types(level);
        if (!dish || all.isEmpty())
            return all;
        ResourceLocation id = dishTypes(level).get(typePicked(level));
        return Map.of(id, all.get(id));
    }

    /** What the stock in the centre is made of, or empty with no stock or a part among it. */
    public Optional<Material.Located> stuff(Level level) {
        ItemStack s = centreStack();
        return s.isEmpty() || s.has(FDataComponents.MADE_OF.get()) ? Optional.empty() : Material.find(ClientMaterials.on(level), s.getItem());
    }

    /** The stock to shape, as the shaping code reads it: the centre's stack. */
    public List<ItemStack> stock() {
        return List.of(centreStack());
    }

    /** The piles of the picked shape's slots after the first (a lens, an edge), one stock slot each: containers {@code STOCK + 0..}, empty for a part of one material. */
    public List<ItemStack> extras() {
        return extras(player.level());
    }

    private List<ItemStack> extras(Level level) {
        if (dish)
            return List.of();
        return range(PartBenchBlockEntity.STOCK, extraSlots(level));
    }

    /** The name of the slot the {@code i}th extra stock slot fills, or empty when it is not on show. */
    public String extraSlotName(Level level, int i) {
        return extraSlots(level) > i ? option(level).map(o -> o.shaping().choices().get(i).name()).orElse("") : "";
    }

    /** How many stock slots, besides the centre's, the picked shape's part takes (its slots less the first). */
    public int extraSlots(Level level) {
        return dish || mode.get() != MODE_PART ? 0 : option(level).map(o -> Math.min(PartBenchBlockEntity.STOCK_SLOTS, o.shaping().choices().size())).orElse(0);
    }

    /** The stock slot beside the centre: what a part is shaped from to be fitted to a dish container. */
    public List<ItemStack> side() {
        return range(PartBenchBlockEntity.STOCK, 1);
    }

    private List<ItemStack> range(int from, int n) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < n; i++)
            out.add(table.getItem(from + i));
        return out;
    }

    private static List<ItemStack> filled(List<ItemStack> in) {
        return in.stream().filter(s -> !s.isEmpty()).toList();
    }

    /** Whether there is a hand hammer (a knife, at the Prep Table) to work with. */
    public boolean hasHammer() {
        return !station.usesTool() || workTool(hammer());
    }

    /** The shape chosen, if the choice is on the list. */
    public Optional<PartBenchShaping.Option> option(Level level) {
        List<PartBenchShaping.Option> options = PartBenchShaping.options(shapes(level));
        int p = picked();
        return p >= 0 && p < options.size() ? Optional.of(options.get(p)) : Optional.empty();
    }

    // --- aiming the part's axes --------------------------------------------------------------------

    /** The aimed value of axis {@code i} of the picked shape's part ({@link PartBenchShaping#aimable} order). */
    public float aim(int i) {
        return aim[i].get() / 10f;
    }

    /** The aimed values of {@code option}'s part by axis name. */
    private Map<String, Float> aimed(PartBenchShaping.Option option) {
        Map<String, Float> out = new java.util.HashMap<>();
        // Only the shape picked has an aim; any other is made to its defaults, so a stray index cannot borrow it.
        if (!option(player.level()).filter(option::equals).isPresent())
            return out;
        List<Map.Entry<String, ModularType.Axis>> axes = PartBenchShaping.aimable(option);
        for (int i = 0; i < Math.min(MAX_AIM, axes.size()); i++)
            out.put(axes.get(i).getKey(), aim(i));
        return out;
    }

    /** What the aimed axes make of {@code option}'s stock: the volume scale. */
    public float aimScale(PartBenchShaping.Option option) {
        return PartBenchShaping.roleOf(option).map(r -> r.volumeScaleOf(aimed(option), option.variant().name())).orElse(1f);
    }

    /** A new pick starts every axis at its default (server). */
    private void syncAim(Level level) {
        if (level.isClientSide || aimFor == picked())
            return;
        aimFor = picked();
        // Stock left in a slot the new shape does not have goes back to the player, not stranded out of sight.
        if (!dish && player instanceof ServerPlayer sp)
            for (int i = extraSlots(level); i < MAX_EXTRA_SLOTS; i++) {
                ItemStack left = table.removeItemNoUpdate(PartBenchBlockEntity.STOCK + i);
                if (!left.isEmpty())
                    sp.getInventory().placeItemBackInInventory(left);
            }
        List<Map.Entry<String, ModularType.Axis>> axes = option(level).map(PartBenchShaping::aimable).orElse(List.of());
        for (int i = 0; i < MAX_AIM; i++)
            aim[i].set(i < axes.size() ? Math.round(axes.get(i).getValue().def() * 10f) : 0);
    }

    private void aimClick(Level level, int id) {
        Optional<PartBenchShaping.Option> option = option(level);
        if (option.isEmpty() || mode.get() != MODE_PART || forging >= 0)
            return;
        List<Map.Entry<String, ModularType.Axis>> axes = PartBenchShaping.aimable(option.get());
        int a;
        float value;
        if (id >= BTN_AXIS_SET) {
            a = (id - BTN_AXIS_SET) / 1000;
            if (a >= axes.size() || a >= MAX_AIM)
                return;
            value = axes.get(a).getValue().min() + (id - BTN_AXIS_SET) % 1000 * axes.get(a).getValue().grain();
        } else {
            a = (id - BTN_AXIS) / 2;
            if (a >= axes.size() || a >= MAX_AIM)
                return;
            value = aim(a) + ((id - BTN_AXIS) % 2 == 1 ? 1 : -1) * axes.get(a).getValue().grain();
        }
        aim[a].set(Math.round(axes.get(a).getValue().clamp(value) * 10f));
    }

    /** The as-built axes a part made now gets: the aim plus symmetric noise ({@link PartBenchShaping#spread}), seeded by the game time and the player. */
    private Map<String, Float> built(Level level, PartBenchShaping.Option option, float workRatio, int flaw) {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(level.getGameTime() * 31L + player.getUUID().hashCode());
        return PartBenchShaping.asBuilt(option, aimed(option), PartBenchShaping.spread(option, workRatio, flaw), random);
    }

    // --- breaking stock down ---------------------------------------------------------------------

    /** What a big piece of stock in the centre breaks into (empty when it is no bigger than an ingot): smaller items of the same stuff. */
    public List<ItemStack> breakdown(Level level) {
        Optional<Material.Located> what = stuff(level);
        if (what.isEmpty() || mode.get() != MODE_PART || cutTab())
            return List.of();
        return PartBenchShaping.change(what.get(), FormItems.volume(what.get().form()));
    }

    private void breakDown(ServerPlayer server) {
        List<ItemStack> back = breakdown(server.level());
        if (back.isEmpty() || !hasHammer())
            return;
        Material.Located what = stuff(server.level()).get();
        ItemStack s = centreStack();
        float heat = ItemHeat.get(s, server.level()).value();
        s.shrink(1);
        table.setItem(PartBenchBlockEntity.TOOL, s.isEmpty() ? ItemStack.EMPTY : s);
        for (ItemStack b : back)
            server.getInventory().placeItemBackInInventory(warmed(b, what, heat, server.level()));
        server.level().playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.7f, 1.1f);
        wearHammer();
    }

    /** {@code stack} at the heat the stock had, when the stuff is worked hot. */
    private static ItemStack warmed(ItemStack stack, Material.Located what, float heat, Level level) {
        if (heat > 0 && what.material().minTemperature().value() > 0)
            ItemHeat.set(stack, new io.github.soundgoodizerfan.feedback.core.unit.Tu(heat), level);
        return stack;
    }

    /**
     * Spend what a shape takes of the stock in container slot {@code slot}: whole items, the part of the last one that the
     * shape does not need coming back as smaller items ({@link PartBenchShaping#change}) to the player.
     */
    private void spend(int slot, PartBenchShaping.Stock stock, ServerPlayer server) {
        for (PartBenchShaping.Stock.Extra e : stock.more())
            spendFrom(PartBenchBlockEntity.STOCK + e.index(), e.what(), e.spent(), e.coldest(), server);
        spendFrom(slot, stock.what(), stock.spent(), stock.coldest(), server);
    }

    private void spendFrom(int slot, Material.Located what, int spent, float coldest, ServerPlayer server) {
        PartBenchShaping.Stock stock = new PartBenchShaping.Stock(what, 0, spent, coldest, 0);
        ItemStack s = table.getItem(slot);
        int volume = FormItems.volume(stock.what().form());
        int n = Math.min(s.getCount(), (stock.spent() + volume - 1) / volume);
        int left = n * volume - stock.spent();
        s = s.copy();
        s.shrink(n);
        table.setItem(slot, s.isEmpty() ? ItemStack.EMPTY : s);
        if (left > 0)
            for (ItemStack b : PartBenchShaping.change(stock.what(), left))
                server.getInventory().placeItemBackInInventory(warmed(b, stock.what(), stock.coldest(), server.level()));
    }

    // --- what the contents make ----------------------------------------------------------------

    /** What taking the output would give, from the centre: empty if no hammer or nothing valid. */
    public ItemStack preview(Level level) {
        syncAim(level);
        Centre c = centre(level);
        if (c == Centre.TEMPLATE)
            return smithed(level);
        if (c == Centre.EMPTY || c == Centre.BLOOM)
            return ItemStack.EMPTY;
        if (cutting()) {
            ItemStack in = centreStack();
            if (sawing(level))
                return hasHammer() ? woodCut(level).filter(w -> in.getCount() >= w.inputs(in)).map(w -> w.result().copy()).orElse(ItemStack.EMPTY)
                        : ItemStack.EMPTY;
            if (!hasHammer())
                return ItemStack.EMPTY;
            var all = cuts(level);
            return cutPicked() >= 0 && cutPicked() < all.size() ? all.get(cutPicked()) : ItemStack.EMPTY;
        }
        if (!hasHammer())
            return ItemStack.EMPTY;
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = types(level);
        if (materials.isEmpty() || types.isEmpty())
            return ItemStack.EMPTY;
        return switch (c) {
            case STOCK -> option(level).map(o -> PartBenchShaping.make(station, o, stock(), extras(level), materials, level, aimScale(o), built(level, o, 1f, 0))).orElse(ItemStack.EMPTY);
            default -> ItemStack.EMPTY;
        };
    }

    private int modeOf(Level level) {
        return switch (centre(level)) {
            case PART -> MODE_ASSEMBLE;
            case TOOL -> MODE_PREPARE;
            case TEMPLATE -> MODE_SMITH;
            default -> MODE_PART;
        };
    }

    /** Recompute the mode and the output and tell the player's client, as a crafting grid does. */
    private void refresh(boolean send) {
        seen = ItemStack.hashStackList(range(0, table.getContainerSize()));
        Level level = player.level();
        net.minecraft.world.item.Item now = centreStack().getItem();
        if (now != centreWas) {
            centreWas = now;
            pick.set(-1);
            forging = -1;
        }
        mode.set(modeOf(level));
        ItemStack out = preview(level);
        output.setItem(0, out);
        if (send && player instanceof ServerPlayer server) {
            setRemoteSlot(OUT, out);
            server.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), OUT, out));
        }
    }

    /** The output was taken: spend what made it, and a point of the hammer. */
    private void taken() {
        if (player.level().isClientSide || !(player instanceof ServerPlayer server))
            return;
        Level level = server.level();
        if (smithing()) {
            table.removeItem(PartBenchBlockEntity.TOOL, 1);
            table.removeItem(PartBenchBlockEntity.STOCK, 1);
            table.removeItem(PartBenchBlockEntity.STOCK + 1, 1);
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1f, 1f);
            refresh(true);
            return;
        }
        if (cutting()) {
            ItemStack in = centreStack().copy();
            Optional<WoodCuts.Cut> saw = sawing(level) ? woodCut(level) : Optional.empty();
            // A recipe data stick in the off hand records this cut (the stonecutter alone for now; the other benches call the same with their station id).
            if (station == Station.STONECUTTING) {
                List<ItemStack> made = cuts(level);
                ItemStack result = saw.map(c -> c.result()).orElse(cutPicked() >= 0 && cutPicked() < made.size() ? made.get(cutPicked()) : ItemStack.EMPTY);
                io.github.soundgoodizerfan.feedback.item.DataStickItem.record(player, level, station.getSerializedName(), in, result);
            }
            // The stonecutter's saw is worn by the stock, so it is read before the stock goes.
            if (station == Station.STONECUTTING && saw.isEmpty())
                wearHammer();
            table.removeItem(PartBenchBlockEntity.TOOL, saw.map(c -> c.inputs(in)).orElse(1));
            if (saw.isPresent()) {
                // The last log's spare planks come back to the player, and the axe takes its point of wear.
                int left = saw.get().leftover(in);
                if (left > 0)
                    WoodCuts.planksOf(level, in).ifPresent(p -> player.getInventory().placeItemBackInInventory(new ItemStack(p, left)));
                wearHammer();
            }
            level.playSound(null, pos, saw.isPresent() ? SoundEvents.WOOD_BREAK : SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.8f, 1f);
            refresh(true);
            return;
        }
        switch (mode.get()) {
            case MODE_PART -> {
                Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
                Optional<PartBenchShaping.Option> option = option(level);
                Optional<PartBenchShaping.Stock> stock = option.flatMap(o -> PartBenchShaping.check(station, o, stock(), extras(level), materials, level, aimScale(o)));
                if (stock.isPresent() && !option.get().forged() && BuiltInRegistries.ITEM.get(option.get().shaping().item()) instanceof ModularPartItem)
                    spend(PartBenchBlockEntity.TOOL, stock.get(), server);
                else if (option.isPresent())
                    // A food reshaped: the whole pile goes into the new shape.
                    table.setItem(PartBenchBlockEntity.TOOL, ItemStack.EMPTY);
            }
            default -> { }
        }
        wearHammer();
        refresh(true);
    }

    private void clear(int from, int n) {
        for (int i = 0; i < n; i++)
            table.setItem(from + i, ItemStack.EMPTY);
    }

    /** One point off the hammer: the hand tool's accepted wear. */
    private void wearHammer() {
        wearHammer(1);
    }

    private void wearHammer(int points) {
        if (!station.usesTool())
            return;
        if (station == Station.STONECUTTING)
            points *= sawWear();
        hammer().hurtAndBreak(points, player, EquipmentSlot.MAINHAND);
        table.setChanged();
    }

    /**
     * What an act costs the saw, in points: the stock's hardness over the saw's edge, rounded, at least one. Stock the edge
     * outclasses wears it by the usual point; stock harder than it chews it up, so a diamond-tipped saw outlasts a plain one on rock.
     * The stock is the centre's material, or a cut's input (a vanilla block) when it has one.
     */
    private int sawWear() {
        ModularFigures f = ModularStacks.figures(hammer());
        Optional<Material.Located> what = stuff(player.level());
        if (f == null || what.isEmpty() || f.get(ModularFigures.Fig.HARDNESS) <= 0)
            return 1;
        float ratio = what.get().material().hardness().value() / f.get(ModularFigures.Fig.HARDNESS);
        return Math.max(1, Math.min(io.github.soundgoodizerfan.feedback.core.FTuning.SAW_MAX_WEAR, Math.round(ratio)));
    }

    // --- adding to a dish container, taking one off -------------------------------------------------

    /** One node of a dish container's fan: something fitted, something held that could be, or a food that could be shaped. */
    public record Add(Source source, ItemStack icon, int index, String role) {
        public enum Source { FITTED, HELD, SHAPE }
    }

    /**
     * Everything that can be added to the part in the centre, in the order the fan shows it: what is already fitted (it can
     * come off), then what the player holds that could attach, then every part that could attach and would be shaped from
     * the stock slot. Not cheap: the screen keeps the list until the contents change.
     */
    public List<Add> adds(Level level) {
        List<Add> out = new ArrayList<>();
        if (centre(level) != Centre.PART)
            return out;
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = assemblyTypes(level);
        List<ItemStack> cur = parts();
        List<ItemStack> asm = assembly();
        for (int i = 0; i < asm.size(); i++)
            if (!asm.get(i).isEmpty())
                out.add(new Add(Add.Source.FITTED, asm.get(i), i, ""));
        for (int s = inv(); s < slots.size(); s++) {
            ItemStack held = slots.get(s).getItem();
            if (!held.isEmpty() && attaches(held, cur, materials, types))
                out.add(new Add(Add.Source.HELD, held, s, ""));
        }
        // Everything else that could attach: each shaped part some reachable type has a role for, once.
        List<PartBenchShaping.Option> options = PartBenchShaping.options(shapes(level));
        java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
        ItemStack side = side().get(0);
        Optional<Material.Located> sideStuff = side.isEmpty() ? Optional.empty() : Material.find(materials, side);
        MadeOf first = centreStack().get(FDataComponents.MADE_OF.get());
        for (int i = 0; i < options.size(); i++) {
            PartBenchShaping.Option o = options.get(i);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(o.shaping().item());
            if (!(item instanceof ModularPartItem) || !o.variant().name().isEmpty() || !seen.add(item))
                continue;
            ItemStack stand = new ItemStack(item);
            stand.set(FDataComponents.MADE_OF.get(), sideStuff.map(l -> new MadeOf(l.id(), l.form(), o.cells())).orElse(first));
            if (attaches(stand, cur, materials, types))
                out.add(new Add(Add.Source.SHAPE, new ItemStack(item), i, ""));
        }
        return out;
    }

    /** Whether {@code stack} put with {@code cur} is still on the way to some tool of this bench. */
    private boolean attaches(ItemStack stack, List<ItemStack> cur, Map<ResourceLocation, Material> materials,
                                    Map<ResourceLocation, ModularType> types) {
        List<ItemStack> all = new ArrayList<>(cur);
        all.add(stack);
        return !ModularAssembly.candidates(all, materials, types).isEmpty();
    }

    /** How many pieces fitting {@code stack} takes: one part, or what the roles it could fill ask of raw stock. */
    private int piecesFor(ItemStack stack, List<ItemStack> cur, Map<ResourceLocation, Material> materials, Map<ResourceLocation, ModularType> types) {
        if (stack.getItem() instanceof ModularPartItem)
            return 1;
        Optional<Material.Located> what = Material.find(materials, stack);
        if (what.isEmpty())
            return 1;
        List<ItemStack> all = new ArrayList<>(cur);
        all.add(stack);
        int want = Integer.MAX_VALUE;
        for (ResourceLocation id : ModularAssembly.candidates(all, materials, types)) {
            ModularType type = types.get(id);
            for (Map.Entry<String, ModularType.Role> e : type.roles().entrySet()) {
                ModularType.Role r = e.getValue();
                if (r.after() || r.part().isPresent() || r.striker() || !r.accepts(what.get()))
                    continue;
                // A laid role takes as many as its pattern has cells (a handle's two); an optional one, its bulk.
                int laid = type.forms().stream().filter(f -> f.containsKey(e.getKey())).mapToInt(f -> f.get(e.getKey())).min().orElse(r.bulk());
                want = Math.min(want, laid);
            }
        }
        return want == Integer.MAX_VALUE ? 1 : want;
    }

    /** Fit what lies in menu slot {@code s} to the part in the centre. */
    private void fit(ServerPlayer server, int s) {
        Level level = server.level();
        if (centre(level) != Centre.PART || s < inv() || s >= slots.size() || !hasHammer())
            return;
        ItemStack held = slots.get(s).getItem();
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = assemblyTypes(level);
        List<ItemStack> cur = parts();
        if (held.isEmpty() || !attaches(held, cur, materials, types))
            return;
        int n = piecesFor(held, cur, materials, types);
        int free = firstFree();
        if (free < 0 || held.getCount() < n)
            return;
        table.setItem(asmIndex(free), held.split(n));
        slots.get(s).setChanged();
        assembled(server);
    }

    /** Shape option {@code index} from the stock slot and fit the part. */
    private void shapeFit(ServerPlayer server, int index) {
        Level level = server.level();
        List<PartBenchShaping.Option> options = PartBenchShaping.options(shapes(level));
        if (centre(level) != Centre.PART || index < 0 || index >= options.size() || !hasHammer() || firstFree() < 0)
            return;
        PartBenchShaping.Option o = options.get(index);
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        if (o.forged() || !(BuiltInRegistries.ITEM.get(o.shaping().item()) instanceof ModularPartItem))
            return;
        Optional<PartBenchShaping.Stock> stock = PartBenchShaping.check(station, o, side(), materials, level, index == picked() ? aimScale(o) : 1f);
        if (stock.isEmpty())
            return;
        Optional<ItemStack> part = PartBenchShaping.part(o, stock.get(), 0, 1f, index == picked() ? built(level, o, 1f, 0) : Map.of(), level);
        if (part.isEmpty() || !attaches(part.get(), parts(), materials, assemblyTypes(level)))
            return;
        spend(PartBenchBlockEntity.STOCK, stock.get(), server);
        table.setItem(asmIndex(firstFree()), part.get());
        wearHammer();
        assembled(server);
    }

    private int firstFree() {
        List<ItemStack> asm = assembly();
        for (int i = 0; i < asm.size(); i++)
            if (asm.get(i).isEmpty())
                return i;
        return -1;
    }

    /** The centre and what is fitted to it make a dish: it becomes the centre. */
    private void assembled(ServerPlayer server) {
        Level level = server.level();
        Optional<ModularAssembly.Match> match = ModularAssembly.infer(parts(), ItemStack.EMPTY, ClientMaterials.on(level), assemblyTypes(level));
        if (match.isPresent()) {
            clear(PartBenchBlockEntity.ASSEMBLY, ModularAssembly.DISH_SLOTS);
            table.setItem(PartBenchBlockEntity.TOOL, match.get().tool());
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
            wearHammer();
        }
        refresh(true);
    }

    /** The axes of the part in the centre that a file can work (empty unless the centre is a part to file). */
    public List<Map.Entry<String, ModularType.Axis>> fileAxes(Level level) {
        return Filing.axes(centreStack(), types(level));
    }

    /** One stroke of a file from the player's inventory on axis {@code index} of the part in the centre; the swarf goes to the player. */
    private void file(ServerPlayer server, int index) {
        Level level = server.level();
        if (centre(level) != Centre.FILE)
            return;
        ItemStack file = ItemStack.EMPTY;
        List<ItemStack> held = new ArrayList<>(server.getInventory().items);
        held.addAll(server.getInventory().offhand);
        for (ItemStack s : held)
            if (!s.isEmpty() && io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities.can(s, io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities.FILE)
                    && !ModularStacks.broken(s)) {
                file = s;
                break;
            }
        if (file.isEmpty()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(server, Component.translatable("gui.feedback.bench.file.no_file"));
            return;
        }
        Filing.Stroke stroke = Filing.stroke(centreStack(), index, types(level), ClientMaterials.on(level));
        if (!stroke.ok()) {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.screen(server, Component.translatable("gui.feedback.bench.file." + stroke.refusal()));
            return;
        }
        table.setItem(PartBenchBlockEntity.TOOL, stroke.part());
        if (!stroke.swarf().isEmpty())
            server.getInventory().placeItemBackInInventory(stroke.swarf());
        if (stroke.wear())
            io.github.soundgoodizerfan.feedback.modular.engineering.ToolAbilities.wear(file, 1);
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.35f, 1.6f);
        table.setChanged();
    }

    /** Take off: a fitted food (index into {@link #assembly}) goes back to the player. */
    private void takeOff(ServerPlayer server, int k) {
        Level level = server.level();
        if (centre(level) != Centre.PART)
            return;
        List<ItemStack> asm = assembly();
        if (k >= 0 && k < asm.size() && !asm.get(k).isEmpty()) {
            server.getInventory().placeItemBackInInventory(table.removeItemNoUpdate(asmIndex(k)));
            table.setChanged();
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.2f);
        }
    }

    // --- buttons: shape, fit, take off --------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        Level level = server.level();
        if (tabs.click(id)) {
            // A new tab starts clean: no shape chosen, and the addition slot of the Trim tab goes back to the player.
            pick.set(-1);
            forging = -1;
            if (!trim())
                server.getInventory().placeItemBackInInventory(table.removeItemNoUpdate(PartBenchBlockEntity.STOCK + 1));
            table.setChanged();
        } else if (id >= BTN_FILE) {
            file(server, id - BTN_FILE);
        } else if (id >= BTN_AXIS) {
            aimClick(level, id);
        } else if (id >= BTN_OFF) {
            takeOff(server, id - BTN_OFF);
        } else if (id >= BTN_SHAPE) {
            shapeFit(server, id - BTN_SHAPE);
        } else if (id >= BTN_FIT) {
            fit(server, id - BTN_FIT);
        } else if (id >= BTN_CUT) {
            if (cuts(level).size() > id - BTN_CUT)
                pick.set(id);
            forging = -1;
        } else if (id >= BTN_PICK) {
            pick.set(id - BTN_PICK);
            forging = -1;
        } else if (id >= BTN_TYPE) {
            if (!dish || id - BTN_TYPE >= dishTypes(level).size())
                return false;
            dishType.set(id - BTN_TYPE);
        } else if (id == BTN_MACHINE && !dish) {
            machineForge(server);
        } else if (id == BTN_DESELECT) {
            pick.set(-1);
            forging = -1;
        } else if (id == BTN_BREAK) {
            breakDown(server);
        } else if (id == BTN_MAKE) {
            ItemStack made = output.getItem(0).copy();
            if (made.isEmpty())
                return false;
            taken();
            server.getInventory().placeItemBackInInventory(made);
        } else
            return false;
        syncAim(level);
        refresh(true);
        return true;
    }

    // --- the Prepare tab (Prep Table), from the screens -----------------------------------------

    /** The food or the finished dish in the work slot (the tool slot at the Prep Table). */
    public ItemStack work() {
        return table.getItem(PartBenchBlockEntity.TOOL);
    }

    /** The seasoning in the seasoning slot (the plan slot at the Prep Table). */
    public ItemStack season() {
        return table.getItem(PartBenchBlockEntity.PLAN);
    }

    /** Whether {@code stack} is a finished dish: a dish-family item with a design. */
    public static boolean isDish(ItemStack stack) {
        return ModularStacks.design(stack) != null && ModularAssembly.kindOf(stack) == ModularAssembly.Kind.DISH;
    }

    /** Whether {@code stack} is a seasoning (poured or sprinkled, not cut), by its material's trait. */
    public static boolean isSeasoning(Level level, ItemStack stack) {
        return Material.find(ClientMaterials.on(level), stack).filter(l -> l.material().traits().contains(Material.Trait.SEASONING)).isPresent();
    }

    /** Whether the dish already has its one finish. */
    public static boolean finished(Level level, ItemStack dish) {
        ModularDesign d = ModularStacks.design(dish);
        ModularType type = d == null ? null : ModularTypes.on(level).get(d.type());
        String after = type == null ? null : type.afterRole();
        return after != null && d.part(after) != null;
    }

    /**
     * Whether the cut (kind 0: a food that is not a seasoning, and a knife), the Finish (kind 1: a finished dish with no
     * finish yet, and a seasoning beside it) or the Knead (kind 2: a kneadable dough) can be played now.
     */
    public boolean preparable(Level level, int kind) {
        if (!dish || work().isEmpty())
            return false;
        if (kind == 0)
            return Cooking.find(level, work()).isPresent() && !isSeasoning(level, work()) && hasHammer();
        // A knead takes dough by its trait, with bare hands: no knife.
        if (kind == 2)
            return Cooking.find(level, work()).filter(Cooking.Entry::kneadable).isPresent();
        return kind == 1 && isDish(work()) && isSeasoning(level, season()) && !finished(level, work());
    }

    /** The player opened a minigame: note when and on what, so {@link #finishPrepare} can refuse a result sent faster than it could be played. */
    public void beginPrepare(ServerPlayer server, int kind) {
        prepKind = -1;
        if (!preparable(server.level(), kind))
            return;
        prepKind = kind;
        prepItem = work().getItem();
        prepSeason = season().getItem();
        prepDesign = ModularStacks.design(work());
        prepStart = server.level().getGameTime();
    }

    /**
     * The cut's mark positions (0..1000), the pour's held ticks or the knead's press samples (0..100). The class is recomputed here from the food's own figures,
     * never taken from the client; a result sent faster than the play could have run is refused, a ragged cut or a flooded
     * pour is applied (a price, not a refusal). Needs a begin, the same food (or the same dish and seasoning) still in the
     * slots, and the knife for a cut.
     */
    public void finishPrepare(ServerPlayer server, int kind, List<Integer> marks, int heldTicks) {
        // The screen already announced its predicted grade; a refusal must take that back.
        if (!applyPrepare(server, kind, marks, heldTicks))
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("prep.feedback.refused"), 60);
    }

    private boolean applyPrepare(ServerPlayer server, int kind, List<Integer> marks, int heldTicks) {
        int begun = prepKind;
        prepKind = -1;
        Level level = server.level();
        if (begun != kind || !preparable(level, kind) || work().getItem() != prepItem)
            return false;
        long elapsed = level.getGameTime() - prepStart + FTuning.PREP_SLACK_TICKS;
        Component said;
        if (kind == 0) {
            ItemStack done = work().copy();
            Cooking.Entry entry = Cooking.find(level, work()).get();
            if (!PrepWork.valid(marks) || (long) marks.size() * FTuning.CUT_TICKS_PER_MARK > elapsed)
                return false;
            int[] landed = marks.stream().mapToInt(m -> PrepWork.land(m, entry.food(), PrepWork.Edge.of(hammer()))).toArray();
            FoodCut cut = PrepWork.cut(landed, landed.length);
            if (cut.level() == 0)
                done.remove(FDataComponents.FOOD_CUT.get());
            else {
                done.set(FDataComponents.FOOD_CUT.get(), cut);
                wearHammer();
            }
            Cooking.apply(done, entry, Cooking.state(done, entry));
            said = cut.level() == 0 ? Component.translatable("prep.feedback.cut.whole") : cutName(cut);
            table.setItem(PartBenchBlockEntity.TOOL, done);
        } else if (kind == 2) {
            // The press samples (0..100, one per KNEAD_STEP_TICKS) cannot outnumber the ticks that have passed.
            Optional<Cooking.Entry> entry = Cooking.find(level, work());
            if (entry.isEmpty() || !Kneading.valid(marks) || (long) (marks.size() * FTuning.KNEAD_STEP_TICKS * FTuning.KNEAD_TIME_SHARE) > elapsed)
                return false;
            int grade = Kneading.grade(entry.get().food(), marks);
            ItemStack done = work().copy();
            done.set(FDataComponents.FOOD_KNEAD.get(), new FoodKnead(grade));
            table.setItem(PartBenchBlockEntity.TOOL, done);
            said = Component.translatable("prep.feedback.knead." + grade);
        } else {
            ItemStack seasoning = season();
            Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
            Optional<Material.Located> what = Material.find(materials, seasoning);
            ModularDesign design = ModularStacks.design(work());
            ModularType type = design == null ? null : ModularTypes.on(level).get(design.type());
            if (seasoning.getItem() != prepSeason || design == null || !design.equals(prepDesign) || what.isEmpty() || type == null
                    || type.afterRole() == null)
                return false;
            // Poured: held ticks. Shaken: heldTicks is the sweeps. Placed: the marks are the pieces. Each is bounded by the time it takes.
            int mode = PrepWork.mode(what.get().material());
            float strength;
            if (mode == PrepWork.PLACE) {
                if (marks.isEmpty() || !PrepWork.validPlaced(marks) || (long) marks.size() * FTuning.CUT_TICKS_PER_MARK > elapsed)
                    return false;
                strength = PrepWork.placed(marks);
            } else if (mode == PrepWork.SHAKE) {
                if (heldTicks <= 0 || heldTicks > FTuning.SHAKE_MAX_STROKES || (long) heldTicks * FTuning.SHAKE_TICKS_PER_STROKE > elapsed)
                    return false;
                strength = PrepWork.shaken(heldTicks);
            } else {
                if (heldTicks <= 0 || heldTicks > FTuning.POUR_MAX_TICKS || heldTicks > elapsed)
                    return false;
                strength = PrepWork.strength(PrepWork.poured(heldTicks, PrepWork.flow(what.get().material())));
            }
            // The seasoning joins the design as its finish part, carrying its state and any cut, plus how it was poured.
            ModularDesign.Part base = ModularAssembly.part(type.afterRole(), what.get(), seasoning, 1);
            java.util.Map<String, Float> state = new java.util.HashMap<>(base.measures().state());
            state.put("pour", strength);
            List<ModularDesign.Part> parts = new ArrayList<>(design.parts());
            parts.add(new ModularDesign.Part(base.role(), base.material(), base.form(), base.count(), base.kind(), base.variant(), base.flaw(), base.measures().withState(state)));
            ItemStack done = work().copy();
            if (!ModularStacks.assemble(done, design.withParts(parts), materials, ModularTypes.on(level)))
                return false;
            table.setItem(PartBenchBlockEntity.PLAN, ItemStack.EMPTY);
            table.setItem(PartBenchBlockEntity.TOOL, done);
            said = pourName(strength);
        }
        io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, said, 60);
        refresh(true);
        return true;
    }

    /** "Sliced, even": a cut in words. Shared with the food's tooltip. */
    public static net.minecraft.network.chat.MutableComponent cutName(FoodCut cut) {
        return Component.translatable("prep.feedback.cut", Component.translatable("prep.feedback.cut.level." + cut.level()),
                Component.translatable("prep.feedback.cut.grade." + cut.grade()));
    }

    /** "Light pour": a pour strength in words. Shared with the food's tooltip. */
    public static net.minecraft.network.chat.MutableComponent pourName(float strength) {
        return Component.translatable("prep.feedback.pour." + PrepWork.pourClass(strength));
    }

    // --- forging, from the screen --------------------------------------------------------------

    /**
     * The player began playing shape {@code index}'s pattern. The server notes when and how hot the
     * stock is, so it can judge the blows against the same heat clock the screen played and refuse a
     * result sent faster than the pattern runs. Refused unless the stock, the hammer and the shape
     * are all as {@link #finishForge} will need them.
     */
    public void beginForge(ServerPlayer server, int index) {
        Level level = server.level();
        Optional<PartBenchShaping.Stock> stock = forgeable(level, index);
        if (stock.isEmpty())
            return;
        Material material = stock.get().what().material();
        if (!ForgeRhythm.workable(material, stock.get().coldest(), hold())) {
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("forge.feedback.unworkable"), 60);
            return;
        }
        if (!ForgeRhythm.movable(optionAt(level, index).variant().rhythm(), strikes(), material)) {
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("forge.feedback.unmovable"), 60);
            return;
        }
        forging = index;
        ForgeRhythm.begin(server.getUUID(), new ForgeRhythm.Session(optionAt(level, index).variant().name(), level.getGameTime(), stock.get().coldest()));
    }

    /**
     * The blows struck, in ticks from the start. The server judges them against the pattern and the
     * stock's heat, makes the part with the flaws the playing earned and hands it over. A client that
     * sends a result faster than the pattern runs is refused; one that sends perfect hits is not
     * caught -- the part is its own player's to spoil, and nothing is lost to anyone else.
     */
    public void finishForge(ServerPlayer server, int index, List<ForgeRhythm.Hit> hits) {
        ForgeRhythm.Session session = ForgeRhythm.end(server.getUUID());
        int begun = forging;
        forging = -1;
        Level level = server.level();
        Optional<PartBenchShaping.Stock> stock = forgeable(level, index);
        if (session == null || begun != index || stock.isEmpty())
            return;
        PartBenchShaping.Option option = optionAt(level, index);
        Material material = stock.get().what().material();
        int step = ForgeRhythm.step(material);
        if (level.getGameTime() - session.started() < ForgeRhythm.end(option.variant().rhythm(), step) - 10)
            return;
        int[] live = ForgeRhythm.live(material, session.temperature(), hold());
        net.minecraft.util.RandomSource noise = ForgeRhythm.noiseFor(hits);
        ForgeRhythm.Result result = ForgeRhythm.judge(option.variant().rhythm(), step, hits, live, noise);
        ForgeRhythm.Work work = ForgeRhythm.work(option.variant().rhythm(), strikes(), material, hits, live, noise,
                new ForgeRhythm.Tempo(io.github.soundgoodizerfan.feedback.combat.skill.Skills.forgeTempoCap(server),
                        io.github.soundgoodizerfan.feedback.combat.skill.Skills.forgeSteady(server)), step);
        if (work.bestStreak() >= ForgeRhythm.TEMPO_STREAK)
            io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.forged(server, option.variant().name());
        complete(server, option, stock.get(), result, work, 1 + work.wasted());
    }

    /**
     * A Mechanical Hammer beside the table plays the shape's pattern itself: every note struck on its
     * tick, at the one force its throw and gearing give, so what it makes is consistent and never perfect (the
     * noise floor is drawn from the game time as well as the blows, so no two runs are the same part).
     * It cannot reheat the part, so a pattern that outlasts the heat leaves dead notes like any other, and
     * a blow it cannot land wears the head ({@link MechanicalHammerBlockEntity#absorb}), not the hand
     * hammer. The result is handed over at once. Needs the table's hammer slot filled as the hand route does,
     * which is {@code [OPEN]} (spec 4.1).
     */
    private void machineForge(ServerPlayer server) {
        Level level = server.level();
        int index = pick.get();
        MechanicalHammerBlockEntity machine = machineBeside();
        Optional<PartBenchShaping.Stock> stock = forgeable(level, index);
        if (machine == null || stock.isEmpty())
            return;
        PartBenchShaping.Option option = optionAt(level, index);
        Material material = stock.get().what().material();
        String rhythm = option.variant().rhythm();
        ForgeRhythm.Strikes strikes = strikes();
        if (!ForgeRhythm.workable(material, stock.get().coldest(), hold())) {
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("forge.feedback.unworkable"), 60);
            return;
        }
        if (!ForgeRhythm.movable(rhythm, strikes, material)) {
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("forge.feedback.unmovable"), 60);
            return;
        }
        int step = ForgeRhythm.step(material);
        List<ForgeRhythm.Hit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < rhythm.length(); i++)
            if (ForgeRhythm.note(rhythm.charAt(i)))
                hits.add(new ForgeRhythm.Hit(ForgeRhythm.noteTick(i, step), rhythm.charAt(i) == 'h'));
        int[] live = ForgeRhythm.live(material, stock.get().coldest(), hold());
        net.minecraft.util.RandomSource noise = net.minecraft.util.RandomSource.create(level.getGameTime() ^ hits.hashCode());
        ForgeRhythm.Result result = ForgeRhythm.judge(rhythm, step, hits, live, noise);
        ForgeRhythm.Work work = ForgeRhythm.work(rhythm, strikes, material, hits, live, noise);
        machine.absorb(work.absorbed());
        complete(server, option, stock.get(), result, work, 0);
    }

    /** Make the part the playing earned and hand it over; {@code hammerPoints} come off the hand hammer. */
    private void complete(ServerPlayer server, PartBenchShaping.Option option, PartBenchShaping.Stock stock,
                          ForgeRhythm.Result result, ForgeRhythm.Work work, int hammerPoints) {
        Level level = server.level();
        Optional<ItemStack> part = PartBenchShaping.part(option, stock, result.flaw(), work.ratio(), built(level, option, work.ratio(), result.flaw()), level);
        if (part.isEmpty())
            return;
        spend(PartBenchBlockEntity.TOOL, stock, server);
        ItemStack made = PartBenchShaping.finished(option, stock, part.get(), ClientMaterials.on(level), level);
        server.getInventory().placeItemBackInInventory(made);
        if (hammerPoints > 0)
            wearHammer(hammerPoints);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
        Component said = Component.translatable("forge.feedback.result." + result.flaw(), made.getHoverName());
        if (work.wasted() > 0)
            said = said.copy().append(" ").append(Component.translatable("forge.feedback.wasted", work.wasted()));
        io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, said, 80);
        refresh(true);
    }

    // --- shingling, from the screen ------------------------------------------------------------

    /** A shingling in play: when it began, how hot the bloom was, and what it began as, so the finish names the same piece. */
    private record Shingle(long started, float temperature, long seed, int slag, int hash) { }

    private static final java.util.Map<java.util.UUID, Shingle> SHINGLES = new java.util.concurrent.ConcurrentHashMap<>();

    /** The layout of a bloom's slag: from its grade and the tick it came out of the fire, which both sides can read. */
    public static long shingleSeed(ItemStack bloom) {
        return 7919L * io.github.soundgoodizerfan.feedback.item.BloomItem.slag(bloom)
                + 31L * bloom.getOrDefault(FDataComponents.HEATED_AT.get(), 0L);
    }

    /** The material figures and hammer a shingling of {@code bloom} is played with, as the screen and the server both build them. */
    public Optional<Shingling.Setup> shingleSetup(Level level, ItemStack bloom) {
        var materials = ClientMaterials.on(level);
        var located = Material.find(materials, bloom.getItem());
        var slag = materials.get(io.github.soundgoodizerfan.feedback.Feedback.id("bloomery_slag"));
        if (located.isEmpty() || slag == null)
            return Optional.empty();
        return Optional.of(Shingling.Setup.of(shingleSeed(bloom), io.github.soundgoodizerfan.feedback.item.BloomItem.slag(bloom),
                ItemHeat.get(bloom, level).value(), hold(), located.get().material(), slag, strikes()));
    }

    /** Whether a bloom sits in the centre, a hammer is in, and the bloom is hot enough to work. */
    private Optional<Shingling.Setup> shingleable(Level level) {
        if (centre(level) != Centre.BLOOM || !hasHammer())
            return Optional.empty();
        ItemStack bloom = centreStack();
        return shingleSetup(level, bloom).filter(s -> s.temperature() >= s.minWork() || s.hold() >= s.minWork());
    }

    public void beginShingle(ServerPlayer server) {
        Level level = server.level();
        Optional<Shingling.Setup> setup = shingleable(level);
        if (setup.isEmpty()) {
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("forge.feedback.unworkable"), 60);
            return;
        }
        ItemStack bloom = centreStack();
        SHINGLES.put(server.getUUID(), new Shingle(level.getGameTime(), setup.get().temperature(), setup.get().seed(),
                setup.get().slag(), ItemStack.hashItemAndComponents(bloom)));
    }

    /**
     * The inputs of the shingling, replayed on the same piece the server noted. A result sent before the last blow could have been struck is
     * refused. A finished piece is wrought iron (an ingot, a ninth lost to each split) with the slag it let out; an unfinished one goes back
     * as a bloom carrying the slag it still holds, to be heated and worked again.
     */
    public void finishShingle(ServerPlayer server, List<Shingling.Event> events) {
        Shingle begun = SHINGLES.remove(server.getUUID());
        Level level = server.level();
        Optional<Shingling.Setup> setup = shingleable(level);
        if (begun == null || setup.isEmpty() || ItemStack.hashItemAndComponents(centreStack()) != begun.hash())
            return;
        long elapsed = level.getGameTime() - begun.started();
        if (!events.isEmpty() && events.get(events.size() - 1).tick() > elapsed + 10)
            return;
        Shingling.Setup played = new Shingling.Setup(begun.seed(), begun.slag(), begun.temperature(), setup.get().hold(), setup.get().slagMelt(),
                setup.get().slagViscosity(), setup.get().minWork(), setup.get().density(), setup.get().hardness(), setup.get().light(), setup.get().heavy());
        Shingling.Result result = Shingling.replay(played, events);
        int end = events.isEmpty() ? 0 : events.get(events.size() - 1).tick();
        var heat = new io.github.soundgoodizerfan.feedback.core.unit.Tu(new Shingling(played).temperature(end));
        var materials = ClientMaterials.on(level);
        var iron = materials.get(io.github.soundgoodizerfan.feedback.Feedback.id("wrought_iron"));
        ItemStack bloom = centreStack();
        List<ItemStack> given = new java.util.ArrayList<>();
        ItemStack main;
        int scale = io.github.soundgoodizerfan.feedback.item.BloomItem.scale(bloom);
        // A hearth that holds the piece over scaling heat for long enough burns it away.
        Shingling clock = new Shingling(played);
        int hotTicks = 0;
        for (int t = 0; t <= end; t++)
            if (clock.temperature(t) > FTuning.SHINGLE_SCALE_TU)
                hotTicks++;
        boolean burnt = hotTicks >= FTuning.SHINGLE_BURN_TICKS || scale >= FTuning.SHINGLE_SCALE_LIMIT;
        if (burnt) {
            main = new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.BURNT_IRON.get());
        } else if (result.done() && iron != null) {
            int mb = Math.max(0, Shingling.metalMb(result) - 16 * scale);
            var ingot = iron.item(io.github.soundgoodizerfan.feedback.Feedback.id("wrought_iron"), "ingot");
            var nugget = iron.item(io.github.soundgoodizerfan.feedback.Feedback.id("wrought_iron"), "nugget");
            main = mb >= 144 && ingot.isPresent() ? new ItemStack(ingot.get()) : ItemStack.EMPTY;
            if (nugget.isPresent() && mb % 144 > 0)
                given.add(new ItemStack(nugget.get(), (mb % 144) / 16));
        } else {
            // Working it again costs iron: each reheat scales a nugget's worth off (the scale is burnt iron).
            main = io.github.soundgoodizerfan.feedback.item.BloomItem.of(bloom.getItem(), Shingling.slagLeft(result), scale + 1);
            given.add(new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.BURNT_IRON.get()));
        }
        for (ItemStack s : given)
            ItemHeat.set(s, heat, level);
        if (!main.isEmpty())
            ItemHeat.set(main, heat, level);
        int slag = Shingling.slagDropped(result);
        if (slag > 0)
            given.add(new ItemStack(io.github.soundgoodizerfan.feedback.registry.FItems.BLOOMERY_SLAG.get(), slag));
        bloom.shrink(1);
        if (bloom.isEmpty())
            table.setItem(PartBenchBlockEntity.TOOL, main);
        else if (!main.isEmpty())
            given.add(main);
        for (ItemStack s : given)
            server.getInventory().placeItemBackInInventory(s);
        wearHammer(1 + events.size() / 8);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
        io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server,
                Component.translatable(result.done() ? "forge.feedback.shingle.done" : "forge.feedback.shingle.left"), 80);
        refresh(true);
    }

    /** A Mechanical Hammer touching the table that is being driven and has a head seated, if there is one. */
    private MechanicalHammerBlockEntity machineBeside() {
        Level level = player.level();
        for (Direction d : Direction.values())
            if (level.getBlockEntity(pos.relative(d)) instanceof MechanicalHammerBlockEntity m && m.isRunning() && !m.getHead().isEmpty())
                return m;
        return null;
    }

    /** Whether a driven Mechanical Hammer stands beside the table, and so plays the pattern. */
    public boolean machine() {
        return machineBeside() != null;
    }

    /** The blows whoever plays the pattern lands: the driven hammer's one force (throw times gearing, to its ceiling), else the player's two. */
    public ForgeRhythm.Strikes strikes() {
        MechanicalHammerBlockEntity m = machineBeside();
        CrankLinkageBlockEntity link = m == null ? null : CrankLinkageBlockEntity.driving(player.level(), m.getBlockPos());
        if (link == null)
            return ForgeRhythm.Strikes.hand(player, hammer());
        St st = new St(Math.min(m.getStrength(link.getThrow()).value() * link.getGearAdvantage(), m.getMaxStrength().value()));
        return new ForgeRhythm.Strikes(st, st);
    }

    private PartBenchShaping.Option optionAt(Level level, int index) {
        return PartBenchShaping.options(shapes(level)).get(index);
    }

    /** The stock, if shape {@code index} is a forged one and there is a hammer and exactly its stock. */
    private Optional<PartBenchShaping.Stock> forgeable(Level level, int index) {
        List<PartBenchShaping.Option> options = PartBenchShaping.options(shapes(level));
        if (mode.get() != MODE_PART || !hasHammer() || index < 0 || index >= options.size() || !options.get(index).forged())
            return Optional.empty();
        return PartBenchShaping.check(station, options.get(index), stock(), index == picked() ? extras(level) : List.of(), ClientMaterials.on(level), level, index == picked() ? aimScale(options.get(index)) : 1f);
    }

    /**
     * Another player at the same table changes a slot through their own menu, so this one is never
     * told: each tick, if the contents are not what the output was made from, make it again.
     */
    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide) {
            holdTu.set(station != Station.ANVIL ? 0 : Math.round(io.github.soundgoodizerfan.feedback.machine.hearth.HearthBlockEntity.holdBeside(player.level(), pos)));
            int now = ItemStack.hashStackList(range(0, table.getContainerSize()));
            if (now != seen) {
                seen = now;
                refresh(true);
            }
        }
        super.broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        // Slots tell the table's own wrapper; nothing else here makes the output.
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (dish)
            clearContainer(player, paper);
        SHINGLES.remove(player.getUUID());
        ForgeRhythm.end(player.getUUID());
    }

    @Override
    public boolean stillValid(Player player) {
        if (station != Station.TOOL)
            return player.level().getBlockState(pos).getBlock() instanceof StationBlock b && b.station == station
                    && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, FModular.PREP_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != output && super.canTakeItemForPickAll(stack, slot);
    }

    /** Stock of one item at slightly different heats is blended to one heat when it meets in the stock slot, so it stacks. */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slotId >= STOCK && slotId < STOCK + PartBenchBlockEntity.STOCK_SLOTS)
            ItemHeat.blend(getCarried(), slots.get(slotId).getItem(), player.level());
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        if (index >= inv())
            ItemHeat.blend(held, table.getItem(PartBenchBlockEntity.STOCK), player.level());
        ItemStack moved = held.copy();
        if (index == OUT) {
            if (!moveItemStackTo(held, inv(), slots.size(), true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(held, moved);
        } else if (index < inv()) {
            if (!moveItemStackTo(held, inv(), slots.size(), false))
                return ItemStack.EMPTY;
        } else if (workTool(held)) {
            if (!moveItemStackTo(held, HAMMER, HAMMER + 1, false))
                return ItemStack.EMPTY;
        } else {
            // From the inventory: a tool to the hammer slot, a seasoning to the seasoning slot, else the centre, then the stock slot.
            if (blueprintTab() && held.is(io.github.soundgoodizerfan.feedback.registry.FTags.BLUEPRINT_PAPER))
                return moveItemStackTo(held, EXTRA, EXTRA + 1, false) ? moved : ItemStack.EMPTY;
            boolean plan = dish && isSeasoning(player.level(), held);
            if (trim() ? !moveItemStackTo(held, EXTRA, EXTRA + 3, false)
                    : !(plan && moveItemStackTo(held, PLAN, PLAN + 1, false))
                    && !moveItemStackTo(held, TOOL, TOOL + 1, false) && !moveItemStackTo(held, STOCK, STOCK + (station.smiths() ? 2 : 1), false))
                return ItemStack.EMPTY;
        }
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (held.getCount() == moved.getCount())
            return ItemStack.EMPTY;
        slot.onTake(player, held);
        if (index == OUT)
            player.drop(held, false);
        return moved;
    }

    /**
     * The Blueprint tab's Print: bake {@code design}, a dish type only (this bench draws food and nothing else), and write it onto the paper in
     * the tab's slot. Food needs no treatise. The server checks everything: the tab is open, the type is a dish, the design bakes.
     */
    public void printBlueprint(ModularDesign design, List<ModularDesign> variants, String title, Player player) {
        if (!blueprintTab())
            return;
        ModularType drawn = ModularTypes.get().types().get(design.type());
        if (drawn == null || drawn.family() != ModularType.Family.DISH) {
            BlueprintTableMenu.send(player, Component.translatable("message.feedback.blueprint_table.cannot_print"));
            return;
        }
        if (BlueprintTableMenu.write(paper, design, variants, title, player))
            broadcastChanges();
    }

    /** The Blueprint tab's paper, or a blueprint printed onto it. */
    public ItemStack paper() {
        return paper.getItem(0);
    }

    /** The heat a Hearth beside this table holds the stock at: read from the world on the server, from the synced slot on the client. */
    public float hold() {
        return holdTu.get();
    }

    /** The coldest piece of stock in the cells, as this side sees it: what the screen glows by. */
    public float heat(Level level) {
        float coldest = Float.MAX_VALUE;
        for (ItemStack s : stock())
            if (!s.isEmpty())
                coldest = Math.min(coldest, ItemHeat.get(s, level).value());
        return coldest == Float.MAX_VALUE ? 0 : coldest;
    }
}
