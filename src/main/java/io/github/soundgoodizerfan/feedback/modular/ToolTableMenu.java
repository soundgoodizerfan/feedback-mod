// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntSupplier;
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
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
 * ({@code docs/gui/gui_bench.md}). The mode below is derived from it on the server each time the contents change.
 *
 * <ul>
 *   <li><b>Stock</b> in the centre ({@link Centre#STOCK}) -- the shapes it can become ({@link ToolTableShaping}), the
 *       cuts the vanilla stonecutter or the saw make of it, and "break down" into smaller stock. A shape spends only
 *       the volume it needs and the rest comes back as smaller items ({@link ToolTableShaping#change}). A forged shape
 *       is played in the screen ({@link #beginForge}, {@link #finishForge}).</li>
 *   <li>A <b>part</b> in the centre ({@link Centre#PART}) -- everything that can be added to it ({@link #adds}); a node
 *       fits a part from the inventory ({@link #BTN_FIT}) or shapes one from the stock slot ({@link #BTN_SHAPE}).
 *       Fitted parts sit in the assembly slots and come off again ({@link #BTN_OFF}); the moment they make a tool it
 *       is the centre ({@link ToolTableAssembly}).</li>
 *   <li>A <b>tool</b> in the centre ({@link Centre#TOOL}) -- its parts, each swapped, mended or taken off
 *       ({@link ModularRework}, {@link #BTN_SWAP}); a tool is a part with others fitted, and taking one off leaves
 *       exactly that.</li>
 * </ul>
 *
 * <p>The output is computed from the contents on the server and sent to the client the way a
 * crafting grid does; taking it spends what made it, and a point of the hammer's durability
 * (hand-tool wear, the accepted exception). No hammer in the hammer slot, no work.
 */
public class ToolTableMenu extends AbstractContainerMenu {

    /** Menu slot numbers; the table's own container is {@link ToolTableBlockEntity}'s numbering. */
    public static final int OUT = 0, HAMMER = 1, ASM = 2, STOCK = ASM + ToolTableAssembly.SLOTS,
            TOOL = STOCK + ToolTableBlockEntity.STOCK_SLOTS, PLAN = TOOL + 1, EXTRA = PLAN + 1;
    /** What the bench is doing, set from the centre: stock shapes, a part being added to, a tool being edited ({@link #MODE_PREPARE} at the Prep Table), a smithing template. */
    public static final int MODE_PART = 0, MODE_ASSEMBLE = 1, MODE_REWORK = 2, MODE_PREPARE = MODE_REWORK, MODE_SMITH = 4;
    /** What the centre slot holds. */
    public enum Centre { EMPTY, STOCK, PART, TOOL, TEMPLATE, BLOOM }
    /**
     * Button ids. {@code PICK + n} chooses shape n, {@code CUT + n} cut n, {@code FIT + s} fits what lies in menu slot s,
     * {@code SHAPE + n} shapes option n from the stock slot and fits it, {@code OFF + k} takes off fitted part k (or tool
     * role k), {@code SWAP + role * 256 + s} puts the stack in menu slot s on a tool's role, {@code TYPE + n} chooses dish type n.
     */
    public static final int BTN_APART = 3, BTN_MACHINE = 4, BTN_BREAK = 5, BTN_MAKE = 6, BTN_DESELECT = 7, BTN_TAB = 8, BTN_TYPE = 50, BTN_PICK = 100, BTN_CUT = 10000,
            BTN_FIT = 20000, BTN_SHAPE = 30000, BTN_OFF = 40000, BTN_SWAP = 50000, BTN_AXIS = 58000, BTN_AXIS_SET = 60000;
    /**
     * Aim buttons for the picked shape's part axes ({@link ToolTableShaping#aimable}, at most {@link #MAX_AIM}):
     * {@code AXIS + 2a} lowers axis a by its grain, {@code AXIS + 2a + 1} raises it; {@code AXIS_SET + 1000a + n} sets it to
     * its minimum plus n grains (a drag). Clamped to the axis's range on the server.
     */
    public static final int MAX_AIM = 4;

    /** The bench: wide, a fan above and the inventory below; the tool slots in a column on the right. */
    public static final int W = 360, H = 262, INV_X = 99, INV_Y = 180;
    /** The slot the work sits in, at the middle of the fan's canvas (item position). */
    public static final int CENTRE_X = 152, CENTRE_Y = 86;
    public static final int HAMMER_X = 326, HAMMER_Y = 20, STOCK_X = 326, STOCK_Y = 50, STOCK1_Y = 70, PLAN_X = 326, PLAN_Y = 96, ROUND_X = 326, ROUND_Y = 126,
            OUT_X = 326, OUT_Y = 152, TRIM_X = 108, TRIM_GAP = 44;
    /** The Blueprint tab's paper slot and the height its board gets: the board fills the bench above the hotbar's row. */
    public static final int PAPER_X = 294, PAPER_Y = INV_Y + 58, BOARD_H = INV_Y + 58;

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
    /** Whether the plan slot is in use now: the dish table's seasoning, a blueprint the centre's type needs, or one already in it. */
    private final DataSlot planShown = DataSlot.standalone();
    /** The heat a Hearth beside the table holds the stock at, in Tu (0 for none): synced so the screen's heat clock agrees with the server's. */
    private final DataSlot holdTu = DataSlot.standalone();
    /** The aimed value of each axis of the picked shape's part, times ten: server-set, synced for the screen's strip and preview. */
    private final DataSlot[] aim = new DataSlot[MAX_AIM];
    /** Server side: the shape the aim was last reset for, so a new pick starts every axis at its default. */
    private int aimFor = -1;
    /** Which table this is: the Tool Table's menu with a type filter, and at the Gun Table a slot for a round ({@link #gunTable}), at the Prep Table a knife for the hammer. */
    private final ToolTableAssembly.Kind kind;
    private final Station station;
    private final boolean gun, dish;
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

    /** Any of the benches: the Gun Table's, the Prep Table's and the part-making stations' menus are this class with their {@code kind}. */
    protected ToolTableMenu(net.minecraft.world.inventory.MenuType<?> type, ToolTableAssembly.Kind kind, int id, Inventory inventory, Container table, BlockPos pos) {
        this(type, kind, id, inventory, table, pos, Station.TOOL);
    }

    /** A part-making bench ({@link StationMenu}): the Tool Table's menu with {@code station} filtering the stock. */
    protected ToolTableMenu(net.minecraft.world.inventory.MenuType<?> type, ToolTableAssembly.Kind kind, int id, Inventory inventory, Container table, BlockPos pos,
                            Station station) {
        super(type, id);
        this.station = station;
        this.kind = kind;
        this.gun = kind == ToolTableAssembly.Kind.GUN;
        this.dish = kind == ToolTableAssembly.Kind.DISH;
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
        addSlot(new TableSlot(ToolTableBlockEntity.HAMMER, HAMMER_X, HAMMER_Y, () -> station.usesTool() && !trim() && !blueprintTab(), 1, this::workTool));
        // The fitted parts: nodes on the fan, not slots on the screen.
        for (int i = 0; i < ToolTableAssembly.SLOTS; i++)
            addSlot(new TableSlot(ToolTableBlockEntity.ASSEMBLY + i, -1000, -1000, () -> false, gun ? 2 : dish ? 1 : ToolTableAssembly.CAPACITY, s -> true));
        for (int i = 0; i < ToolTableBlockEntity.STOCK_SLOTS; i++)
            addSlot(new TableSlot(ToolTableBlockEntity.STOCK + i, i == 0 ? STOCK_X : -1000, STOCK_Y, i == 0 ? () -> !trim() && !cutTab() && !blueprintTab() : () -> false, 64, s -> true));
        // The centre: stock, a part, a tool, or at the Prep Table a food or a dish.
        addSlot(new TableSlot(ToolTableBlockEntity.TOOL, CENTRE_X, CENTRE_Y, () -> !trim() && !blueprintTab(), 64, s -> true));
        // The plan slot: a blueprint, or at the Prep Table the seasoning a finish pours onto the dish.
        addSlot(new TableSlot(ToolTableBlockEntity.PLAN, PLAN_X, PLAN_Y, () -> !trim() && !blueprintTab() && (planShown.get() == 1 || !table.getItem(ToolTableBlockEntity.PLAN).isEmpty()), 1,
                s -> dish ? isSeasoning(player.level(), s) : isPlanItem(s)));
        if (gun) {
            // The rest of a gun's sixteen part slots, and the round its spec sheet is read against (not spent).
            for (int i = ToolTableAssembly.SLOTS; i < ToolTableAssembly.GUN_SLOTS; i++)
                addSlot(new TableSlot(ToolTableBlockEntity.EXTRA + i - ToolTableAssembly.SLOTS, -1000, -1000, () -> false, 1, s -> true));
            addSlot(new TableSlot(ToolTableBlockEntity.ROUND, ROUND_X, ROUND_Y, () -> true, 1,
                    s -> s.is(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE_ITEM.get())));
        }
        if (station.smiths()) {
            // The vanilla smithing table, on the Trim tab: template, base and addition in a row (the same containers the centre and stock slot use).
            addSlot(new TableSlot(ToolTableBlockEntity.TOOL, TRIM_X, CENTRE_Y, this::trim, 64, s -> true));
            addSlot(new TableSlot(ToolTableBlockEntity.STOCK, TRIM_X + TRIM_GAP, CENTRE_Y, this::trim, 64, s -> true));
            addSlot(new TableSlot(ToolTableBlockEntity.STOCK + 1, TRIM_X + 2 * TRIM_GAP, CENTRE_Y, this::trim, 64, s -> true));
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
        addDataSlot(planShown);
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

    /** Which shape of the list is chosen, as an index into {@link ToolTableShaping#options}, or -1 (also while a cut is chosen). */
    public int picked() {
        return pick.get() >= BTN_CUT ? -1 : pick.get();
    }

    /** Which cut is chosen (an index into {@link #cuts}), or -1. */
    public int cutPicked() {
        return pick.get() >= BTN_CUT ? pick.get() - BTN_CUT : -1;
    }

    public ItemStack hammer() {
        return table.getItem(ToolTableBlockEntity.HAMMER);
    }

    /** The centre slot's stack. */
    public ItemStack centreStack() {
        return table.getItem(ToolTableBlockEntity.TOOL);
    }

    /**
     * Whether {@code stack} belongs in the plan slot: a plan sheet, a book of them, or blank paper, which a taken-apart item writes its
     * design onto ({@link #measure}).
     */
    public static boolean isPlanItem(ItemStack stack) {
        return stack.is(FModular.BLUEPRINT.get()) || stack.getItem() instanceof BlueprintBookItem || stack.is(FModular.BLANK_TREATISE.get())
                || stack.is(io.github.soundgoodizerfan.feedback.registry.FTags.BLUEPRINT_PAPER);
    }

    /**
     * Taking a plan-needing item apart with blank paper in the plan slot measures it: the paper becomes a plan sheet of its design (the
     * parts of it that are shaped; a circuit or cell it carried is not part of what a plan pins). The item and the paper both go in.
     */
    private void measure(ServerPlayer server, ItemStack tool, Level level) {
        ItemStack paper = table.getItem(ToolTableBlockEntity.PLAN);
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = design == null ? null : types(level).get(design.type());
        // a blank treatise takes the specimen's school and volume: the item goes in and the knowledge comes out
        School school = type == null ? null : School.of(type.school());
        if (school != null && paper.is(FModular.BLANK_TREATISE.get())) {
            int volume = School.volumeOf(type.school());
            table.setItem(ToolTableBlockEntity.PLAN, Treatises.of(school, volume));
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("message.feedback.treatise.written",
                    Component.translatable(school.itemKey(volume))), 60);
            return;
        }
        if (type == null || !type.blueprint() || paper.isEmpty() || paper.is(FModular.BLUEPRINT.get()) || !paper.is(io.github.soundgoodizerfan.feedback.registry.FTags.BLUEPRINT_PAPER))
            return;
        ModularDesign pinned = new ModularDesign(design.type(), design.parts().stream().filter(p -> p.sub().isEmpty()).toList(), design.edge());
        var figures = ToolDesigns.predict(pinned, types(level), ClientMaterials.on(level));
        table.setItem(ToolTableBlockEntity.PLAN, ModularBlueprints.sheet(pinned, figures.orElse(null)));
        io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("message.feedback.plan.measured"), 60);
    }

    /** The finished tool (or dish) in the centre, else empty. */
    public ItemStack tool() {
        return mode.get() == MODE_REWORK ? centreStack() : ItemStack.EMPTY;
    }

    public ItemStack plan() {
        return table.getItem(ToolTableBlockEntity.PLAN);
    }

    /** The fitted parts: the assembly slots, empty ones included. */
    public List<ItemStack> assembly() {
        List<ItemStack> out = range(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
        if (gun)
            out.addAll(range(ToolTableBlockEntity.EXTRA, ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS));
        return out;
    }

    /** The container index of fitted part {@code i}. */
    private static int asmIndex(int i) {
        return i < ToolTableAssembly.SLOTS ? ToolTableBlockEntity.ASSEMBLY + i : ToolTableBlockEntity.EXTRA + i - ToolTableAssembly.SLOTS;
    }

    /** What is put together at the centre: the part there and every part fitted to it. */
    public List<ItemStack> parts() {
        List<ItemStack> out = new ArrayList<>();
        out.add(centreStack());
        for (ItemStack s : assembly())
            if (!s.isEmpty())
                out.add(s);
        return out;
    }

    /** Whether this is the Gun Table. */
    public boolean gunTable() {
        return gun;
    }

    /** The round in the Gun Table's round slot, or empty. */
    public ItemStack round() {
        return gun ? table.getItem(ToolTableBlockEntity.ROUND) : ItemStack.EMPTY;
    }

    /** The menu slot index where the player's inventory begins. */
    public int inv() {
        return EXTRA + (dish ? 1 : 0) + (gun ? ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS + 1 : 0) + (station.smiths() ? 3 : 0);
    }

    /** Which bench this is: {@link Station#TOOL} at the Gun Table and Prep Table (their menus carry the kind). */
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
        if (ModularStacks.design(c) != null && !c.is(FModular.BLUEPRINT.get()) && ToolTableAssembly.kindOf(c) == kind)
            return Centre.TOOL;
        if (c.getItem() instanceof ModularPartItem && c.has(FDataComponents.MADE_OF.get()))
            return Centre.PART;
        // A bloom (spongy iron with slag in it) is shingled, not shaped: the anvil plays it as its own work.
        if (station.forges() && Material.find(ClientMaterials.on(level), c.getItem()).map(l -> l.form().equals("bloom")).orElse(false))
            return Centre.BLOOM;
        // A flatbread is its own container: it opens the picker like a bowl does.
        if (dish && Material.find(ClientMaterials.on(level), c.getItem()).map(l -> l.material().traits().contains(Material.Trait.WRAPPER)).orElse(false))
            return Centre.PART;
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
                table.getItem(ToolTableBlockEntity.STOCK), table.getItem(ToolTableBlockEntity.STOCK + 1));
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
        return !station.usesTool() ? false : dish ? ToolTableBlockEntity.isKnife(stack) : station == Station.WOODWORKING ? stack.is(net.minecraft.tags.ItemTags.AXES)
                : station == Station.STONECUTTING ? ToolTableBlockEntity.isSaw(stack) : ToolTableBlockEntity.isHammer(stack);
    }

    /** The types this table makes: guns at the Gun Table, dishes at the Prep Table, everything else at the Tool Table. */
    public Map<ResourceLocation, ModularType> types(Level level) {
        return ToolTableAssembly.ofKind(ModularTypes.on(level), kind);
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

    /** The stock slot beside the centre: what mends a tool, swaps one of its parts or makes a part to fit. */
    public List<ItemStack> side() {
        return range(ToolTableBlockEntity.STOCK, 1);
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
    public Optional<ToolTableShaping.Option> option(Level level) {
        List<ToolTableShaping.Option> options = ToolTableShaping.options(shapes(level));
        int p = picked();
        return p >= 0 && p < options.size() ? Optional.of(options.get(p)) : Optional.empty();
    }

    // --- aiming the part's axes --------------------------------------------------------------------

    /** The aimed value of axis {@code i} of the picked shape's part ({@link ToolTableShaping#aimable} order). */
    public float aim(int i) {
        return aim[i].get() / 10f;
    }

    /** The aimed values of {@code option}'s part by axis name. */
    private Map<String, Float> aimed(ToolTableShaping.Option option) {
        Map<String, Float> out = new java.util.HashMap<>();
        // Only the shape picked has an aim; any other is made to its defaults, so a stray index cannot borrow it.
        if (!option(player.level()).filter(option::equals).isPresent())
            return out;
        List<Map.Entry<String, ModularType.Axis>> axes = ToolTableShaping.aimable(option);
        for (int i = 0; i < Math.min(MAX_AIM, axes.size()); i++)
            out.put(axes.get(i).getKey(), aim(i));
        return out;
    }

    /** What the aimed axes make of {@code option}'s stock: the volume scale. */
    public float aimScale(ToolTableShaping.Option option) {
        return ToolTableShaping.roleOf(option).map(r -> r.volumeScaleOf(aimed(option))).orElse(1f);
    }

    /** A new pick starts every axis at its default (server). */
    private void syncAim(Level level) {
        if (level.isClientSide || aimFor == picked())
            return;
        aimFor = picked();
        List<Map.Entry<String, ModularType.Axis>> axes = option(level).map(ToolTableShaping::aimable).orElse(List.of());
        for (int i = 0; i < MAX_AIM; i++)
            aim[i].set(i < axes.size() ? Math.round(axes.get(i).getValue().def() * 10f) : 0);
    }

    private void aimClick(Level level, int id) {
        Optional<ToolTableShaping.Option> option = option(level);
        if (option.isEmpty() || mode.get() != MODE_PART || forging >= 0)
            return;
        List<Map.Entry<String, ModularType.Axis>> axes = ToolTableShaping.aimable(option.get());
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

    /** The as-built axes a part made now gets: the aim plus symmetric noise ({@link ToolTableShaping#spread}), seeded by the game time and the player. */
    private Map<String, Float> built(Level level, ToolTableShaping.Option option, float workRatio, int flaw) {
        net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create(level.getGameTime() * 31L + player.getUUID().hashCode());
        return ToolTableShaping.asBuilt(option, aimed(option), ToolTableShaping.spread(option, workRatio, flaw), random);
    }

    // --- breaking stock down ---------------------------------------------------------------------

    /** What a big piece of stock in the centre breaks into (empty when it is no bigger than an ingot): smaller items of the same stuff. */
    public List<ItemStack> breakdown(Level level) {
        Optional<Material.Located> what = stuff(level);
        if (what.isEmpty() || mode.get() != MODE_PART || cutTab())
            return List.of();
        return ToolTableShaping.change(what.get(), FormItems.volume(what.get().form()));
    }

    private void breakDown(ServerPlayer server) {
        List<ItemStack> back = breakdown(server.level());
        if (back.isEmpty() || !hasHammer())
            return;
        Material.Located what = stuff(server.level()).get();
        ItemStack s = centreStack();
        float heat = ItemHeat.get(s, server.level()).value();
        s.shrink(1);
        table.setItem(ToolTableBlockEntity.TOOL, s.isEmpty() ? ItemStack.EMPTY : s);
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
     * shape does not need coming back as smaller items ({@link ToolTableShaping#change}) to the player.
     */
    private void spend(int slot, ToolTableShaping.Stock stock, ServerPlayer server) {
        ItemStack s = table.getItem(slot);
        int volume = FormItems.volume(stock.what().form());
        int n = Math.min(s.getCount(), (stock.spent() + volume - 1) / volume);
        int left = n * volume - stock.spent();
        s = s.copy();
        s.shrink(n);
        table.setItem(slot, s.isEmpty() ? ItemStack.EMPTY : s);
        if (left > 0)
            for (ItemStack b : ToolTableShaping.change(stock.what(), left))
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
            case STOCK -> option(level).map(o -> ToolTableShaping.make(station, o, stock(), materials, level, aimScale(o), built(level, o, 1f, 0))).orElse(ItemStack.EMPTY);
            case TOOL -> dish ? ItemStack.EMPTY : reworked(level).map(Reworked::out).orElse(ItemStack.EMPTY);
            default -> ItemStack.EMPTY;
        };
    }

    /** A tool mended, swapped or added to from the stock slot: the changed tool, and how many pieces of the stack it took. */
    private record Reworked(ItemStack out, int used) { }

    private Optional<Reworked> reworked(Level level) {
        ItemStack s = side().get(0);
        if (s.isEmpty())
            return Optional.empty();
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        for (int n = 1; n <= Math.min(s.getCount(), ToolTableBlockEntity.STOCK_SLOTS); n++) {
            List<ItemStack> pieces = new ArrayList<>();
            for (int i = 0; i < n; i++)
                pieces.add(s.copyWithCount(1));
            Optional<ItemStack> r = ModularRework.rework(centreStack(), pieces, materials, types(level));
            if (r.isPresent())
                return Optional.of(new Reworked(r.get(), n));
        }
        return Optional.empty();
    }

    /** Whether the centre is a part some blueprint-needing type is built on (the crossbow's prod), so the plan slot is wanted. */
    private boolean needsPlan(Level level) {
        // a blueprint is optional for all forging (it gives credit, never gates), so the slot is always there
        if (!blueprintTab())
            return true;
        ItemStack c = centreStack();
        // a finished item that needs a plan can be measured onto paper put in the plan slot
        ModularDesign made = ModularStacks.design(c);
        if (made != null && types(level).get(made.type()) != null && types(level).get(made.type()).blueprint())
            return true;
        if (!(c.getItem() instanceof ModularPartItem))
            return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(c.getItem());
        for (ModularType t : types(level).values())
            if (t.blueprint())
                for (ModularType.Role r : t.roles().values())
                    if (r.part().isPresent() && r.part().get().item().equals(id))
                        return true;
        return false;
    }

    private int modeOf(Level level) {
        return switch (centre(level)) {
            case PART -> MODE_ASSEMBLE;
            case TOOL -> MODE_REWORK;
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
        planShown.set(needsPlan(level) ? 1 : 0);
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
            table.removeItem(ToolTableBlockEntity.TOOL, 1);
            table.removeItem(ToolTableBlockEntity.STOCK, 1);
            table.removeItem(ToolTableBlockEntity.STOCK + 1, 1);
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
            table.removeItem(ToolTableBlockEntity.TOOL, saw.map(c -> c.inputs(in)).orElse(1));
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
                Optional<ToolTableShaping.Option> option = option(level);
                Optional<ToolTableShaping.Stock> stock = option.flatMap(o -> ToolTableShaping.check(station, o, stock(), materials, level, aimScale(o)));
                if (stock.isPresent() && !option.get().forged() && BuiltInRegistries.ITEM.get(option.get().shaping().item()) instanceof ModularPartItem)
                    spend(ToolTableBlockEntity.TOOL, stock.get(), server);
                else if (option.isPresent())
                    // A food reshaped: the whole pile goes into the new shape.
                    table.setItem(ToolTableBlockEntity.TOOL, ItemStack.EMPTY);
            }
            default -> {
                // A tool reworked: the pieces it took go, and the changed tool is what is taken.
                Optional<Reworked> r = reworked(level);
                ItemStack side = table.getItem(ToolTableBlockEntity.STOCK).copy();
                side.shrink(r.map(Reworked::used).orElse(side.getCount()));
                table.setItem(ToolTableBlockEntity.STOCK, side.isEmpty() ? ItemStack.EMPTY : side);
                table.setItem(ToolTableBlockEntity.TOOL, ItemStack.EMPTY);
            }
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

    // --- adding to a part, taking one off, swapping on a tool -------------------------------------

    /** One node of a part's fan: something fitted, something held that could be, or a part that could be shaped. */
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
        List<ToolTableShaping.Option> options = ToolTableShaping.options(shapes(level));
        java.util.Set<net.minecraft.world.item.Item> seen = new java.util.HashSet<>();
        ItemStack side = side().get(0);
        Optional<Material.Located> sideStuff = side.isEmpty() ? Optional.empty() : Material.find(materials, side);
        MadeOf first = centreStack().get(FDataComponents.MADE_OF.get());
        for (int i = 0; i < options.size(); i++) {
            ToolTableShaping.Option o = options.get(i);
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
        return !ToolTableAssembly.candidates(all, plan(), materials, types).isEmpty();
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
        for (ResourceLocation id : ToolTableAssembly.candidates(all, plan(), materials, types)) {
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
        List<ToolTableShaping.Option> options = ToolTableShaping.options(shapes(level));
        if (centre(level) != Centre.PART || index < 0 || index >= options.size() || !hasHammer() || firstFree() < 0)
            return;
        ToolTableShaping.Option o = options.get(index);
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        if (o.forged() || !(BuiltInRegistries.ITEM.get(o.shaping().item()) instanceof ModularPartItem))
            return;
        Optional<ToolTableShaping.Stock> stock = ToolTableShaping.check(station, o, side(), materials, level, index == picked() ? aimScale(o) : 1f);
        if (stock.isEmpty())
            return;
        Optional<ItemStack> part = ToolTableShaping.part(o, stock.get(), 0, 1f, index == picked() ? built(level, o, 1f, 0) : Map.of(), level);
        if (part.isEmpty() || !attaches(part.get(), parts(), materials, assemblyTypes(level)))
            return;
        spend(ToolTableBlockEntity.STOCK, stock.get(), server);
        table.setItem(asmIndex(firstFree()), part.get());
        wearHammer();
        assembled(server);
    }

    private int firstFree() {
        List<ItemStack> asm = assembly();
        int cap = gun ? ToolTableAssembly.GUN_SLOTS : dish ? ToolTableAssembly.SLOTS : ToolTableAssembly.SLOTS;
        for (int i = 0; i < Math.min(cap, asm.size()); i++)
            if (asm.get(i).isEmpty())
                return i;
        return -1;
    }

    /** The centre and what is fitted to it make a tool: it becomes the centre. */
    private void assembled(ServerPlayer server) {
        Level level = server.level();
        Optional<ToolTableAssembly.Match> match = ToolTableAssembly.infer(parts(), plan(), ClientMaterials.on(level), assemblyTypes(level));
        if (match.isPresent()) {
            clear(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
            if (gun)
                clear(ToolTableBlockEntity.EXTRA, ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS);
            table.setItem(ToolTableBlockEntity.TOOL, match.get().tool());
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
            wearHammer();
        }
        refresh(true);
    }

    /**
     * Take off: a fitted part (index into {@link #assembly}) goes back to the player; on a tool, role {@code k} (an index into the
     * type's order) comes off and the rest of the tool becomes the part in the centre with the others fitted to it -- exactly
     * what it was built from.
     */
    private void takeOff(ServerPlayer server, int k) {
        Level level = server.level();
        Centre c = centre(level);
        if (c == Centre.PART) {
            List<ItemStack> asm = assembly();
            if (k >= 0 && k < asm.size() && !asm.get(k).isEmpty()) {
                server.getInventory().placeItemBackInInventory(table.removeItemNoUpdate(asmIndex(k)));
                table.setChanged();
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.2f);
            }
            return;
        }
        if (c != Centre.TOOL || !hasHammer())
            return;
        ItemStack tool = centreStack();
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = design == null ? null : types(level).get(design.type());
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        if (type == null || k < 0 || k >= type.order().size())
            return;
        String off = type.order().get(k);
        float kept = ModularRework.kept(tool, io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(server, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.SALVAGE_WEAR));
        List<ItemStack> rest = new ArrayList<>();
        boolean gone = false;
        for (ModularDesign.Part part : design.parts()) {
            ModularType.Role role = type.roles().get(part.role());
            if (role == null)
                continue;
            int count = part.role().equals(type.wearRole()) ? (int) Math.floor(part.count() * kept) : part.count();
            List<ItemStack> items = ToolDesigns.items(part, role, materials, count);
            if (part.role().equals(off)) {
                gone = true;
                items.forEach(i -> server.getInventory().placeItemBackInInventory(i));
            } else
                rest.addAll(items);
        }
        if (!gone)
            return;
        // The first shaped part stays in the centre; the rest are fitted to it (and any that do not fit go back to the player).
        table.setItem(ToolTableBlockEntity.TOOL, ItemStack.EMPTY);
        clear(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
        if (gun)
            clear(ToolTableBlockEntity.EXTRA, ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS);
        for (ItemStack i : rest) {
            if (centreStack().isEmpty() && i.getItem() instanceof ModularPartItem)
                table.setItem(ToolTableBlockEntity.TOOL, i);
            else if (firstFree() >= 0 && i.getCount() <= (gun ? 2 : ToolTableAssembly.CAPACITY))
                table.setItem(asmIndex(firstFree()), i);
            else
                server.getInventory().placeItemBackInInventory(i);
        }
        // Armour is its plate: what is left of it is worn as it stands, not a part waiting for something to be fitted.
        if (type.family() == ModularType.Family.ARMOUR)
            ToolTableAssembly.infer(parts(), plan(), materials, types(level)).ifPresent(m -> {
                clear(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
                table.setItem(ToolTableBlockEntity.TOOL, m.tool());
            });
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
        wearHammer();
    }

    /** The menu slots of the player's stacks that role {@code k} of the tool in the centre would take (swap, add or mend). */
    public List<Integer> roleChoices(Level level, int k) {
        List<Integer> out = new ArrayList<>();
        if (dish)
            return out;
        ItemStack tool = centreStack();
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = design == null ? null : types(level).get(design.type());
        if (type == null || k < 0 || k >= type.order().size())
            return out;
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        String key = type.order().get(k);
        ModularType.Role role = type.roles().get(key);
        boolean shaped = role.part().isPresent() || role.striker();
        ModularDesign.Part have = design.part(key);
        int need = shaped ? 1 : have != null ? have.count() : role.bulk();
        for (int s = inv(); s < slots.size(); s++) {
            ItemStack held = slots.get(s).getItem();
            Optional<Material.Located> what = held.isEmpty() ? Optional.empty() : Material.find(materials, held);
            if (what.isEmpty() || held.getCount() < need || !role.accepts(what.get()) && !role.acceptsStock(what.get()))
                continue;
            if (shaped != (held.getItem() instanceof ModularPartItem) && !(key.equals(type.wearRole()) && role.acceptsStock(what.get())))
                continue;
            out.add(s);
        }
        return out;
    }

    /** Put the stack in menu slot {@code s} on role {@code k} of the tool in the centre: a swap, an addition or a mend. */
    private void swap(ServerPlayer server, int k, int s) {
        Level level = server.level();
        if (centre(level) != Centre.TOOL || !hasHammer() || !roleChoices(level, k).contains(s))
            return;
        ItemStack tool = centreStack();
        ModularDesign design = ModularStacks.design(tool);
        ModularType type = types(level).get(design.type());
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        String key = type.order().get(k);
        ModularType.Role role = type.roles().get(key);
        boolean shaped = role.part().isPresent() || role.striker();
        ModularDesign.Part have = design.part(key);
        int need = shaped ? 1 : have != null ? have.count() : role.bulk();
        ItemStack held = slots.get(s).getItem();
        List<ItemStack> given = new ArrayList<>();
        // A mend takes whatever stock is held, as many pieces as the stack has; a swap or an addition the count the role asks.
        boolean mend = have != null && key.equals(type.wearRole()) && !shaped;
        int take = mend ? Math.min(held.getCount(), Math.max(1, need)) : need;
        for (int i = 0; i < take; i++)
            given.add(held.copyWithCount(1));
        Optional<ItemStack> done = ModularRework.rework(tool, given, materials, types(level));
        if (done.isEmpty())
            return;
        // The part that comes off goes back to the player.
        if (have != null && !mend) {
            int count = key.equals(type.wearRole()) ? (int) Math.floor(have.count() * ModularRework.kept(tool, io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(server, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.SALVAGE_WEAR))) : have.count();
            ToolDesigns.items(have, role, materials, count).forEach(i -> server.getInventory().placeItemBackInInventory(i));
        }
        held.shrink(take);
        slots.get(s).setChanged();
        table.setItem(ToolTableBlockEntity.TOOL, done.get());
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.3f);
        wearHammer();
        refresh(true);
    }

    // --- buttons: shape, fit, take off, take apart --------------------------------------------------------

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
                server.getInventory().placeItemBackInInventory(table.removeItemNoUpdate(ToolTableBlockEntity.STOCK + 1));
            table.setChanged();
        } else if (id >= BTN_AXIS) {
            aimClick(level, id);
        } else if (id >= BTN_SWAP) {
            swap(server, (id - BTN_SWAP) / 256, (id - BTN_SWAP) % 256);
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
        } else if (id == BTN_APART) {
            apart(server);
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

    /**
     * Take the tool in the centre apart: its parts come back to the player, and it costs a point of the hammer. At the Prep
     * Table it is a finished dish and a point of the knife; a soup is refused and the finish is lost.
     */
    private void apart(ServerPlayer server) {
        ItemStack tool = tool();
        Level level = server.level();
        if (!hasHammer() || ModularStacks.design(tool) == null || tool.is(FModular.BLUEPRINT.get()) || (dish && !isDish(tool)))
            return;
        List<ItemStack> parts;
        if (dish) {
            Cooking.settle(tool, level, io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value());
            Optional<List<ItemStack>> out = ModularRework.dismantleDish(tool, ClientMaterials.on(level), types(level));
            if (out.isEmpty()) {
                ModularDesign whole = ModularStacks.design(tool);
                boolean baked = whole != null && types(level).get(whole.type()) != null && types(level).get(whole.type()).bakes();
                io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable(baked ? "prep.feedback.apart.baked" : "prep.feedback.apart.soup"), 60);
                return;
            }
            parts = out.get();
            io.github.soundgoodizerfan.feedback.net.HudReadoutPayload.send(server, Component.translatable("prep.feedback.apart"), 60);
        } else {
            parts = ModularRework.dismantle(tool, ClientMaterials.on(level), types(level), io.github.soundgoodizerfan.feedback.combat.skill.Skills.modifier(server, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Figure.SALVAGE_WEAR));
            if (ModularStacks.worn(tool) > 0f)
                io.github.soundgoodizerfan.feedback.combat.skill.SkillEarning.salvaged(server, "apart/" + ModularStacks.design(tool).type());
        }
        if (!dish)
            measure(server, tool, level);
        table.setItem(ToolTableBlockEntity.TOOL, ItemStack.EMPTY);
        parts.forEach(p -> server.getInventory().placeItemBackInInventory(p));
        level.playSound(null, pos, dish ? SoundEvents.AXE_STRIP : SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
        wearHammer();
    }

    // --- the Prepare tab (Prep Table), from the screens -----------------------------------------

    /** The food or the finished dish in the work slot (the tool slot at the Prep Table). */
    public ItemStack work() {
        return table.getItem(ToolTableBlockEntity.TOOL);
    }

    /** The seasoning in the seasoning slot (the plan slot at the Prep Table). */
    public ItemStack season() {
        return table.getItem(ToolTableBlockEntity.PLAN);
    }

    /** Whether {@code stack} is a finished dish: a dish-family item with a design. */
    public static boolean isDish(ItemStack stack) {
        return ModularStacks.design(stack) != null && ToolTableAssembly.kindOf(stack) == ToolTableAssembly.Kind.DISH;
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
            table.setItem(ToolTableBlockEntity.TOOL, done);
        } else if (kind == 2) {
            // The press samples (0..100, one per KNEAD_STEP_TICKS) cannot outnumber the ticks that have passed.
            Optional<Cooking.Entry> entry = Cooking.find(level, work());
            if (entry.isEmpty() || !Kneading.valid(marks) || (long) (marks.size() * FTuning.KNEAD_STEP_TICKS * FTuning.KNEAD_TIME_SHARE) > elapsed)
                return false;
            int grade = Kneading.grade(entry.get().food(), marks);
            ItemStack done = work().copy();
            done.set(FDataComponents.FOOD_KNEAD.get(), new FoodKnead(grade));
            table.setItem(ToolTableBlockEntity.TOOL, done);
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
            ModularDesign.Part base = ToolTableAssembly.part(type.afterRole(), what.get(), seasoning, 1);
            java.util.Map<String, Float> state = new java.util.HashMap<>(base.measures().state());
            state.put("pour", strength);
            List<ModularDesign.Part> parts = new ArrayList<>(design.parts());
            parts.add(new ModularDesign.Part(base.role(), base.material(), base.form(), base.count(), base.kind(), base.variant(), base.flaw(), base.measures().withState(state)));
            ItemStack done = work().copy();
            if (!ModularStacks.assemble(done, new ModularDesign(design.type(), parts, design.edge()), materials, ModularTypes.on(level)))
                return false;
            table.setItem(ToolTableBlockEntity.PLAN, ItemStack.EMPTY);
            table.setItem(ToolTableBlockEntity.TOOL, done);
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
        Optional<ToolTableShaping.Stock> stock = forgeable(level, index);
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
        Optional<ToolTableShaping.Stock> stock = forgeable(level, index);
        if (session == null || begun != index || stock.isEmpty())
            return;
        ToolTableShaping.Option option = optionAt(level, index);
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
        Optional<ToolTableShaping.Stock> stock = forgeable(level, index);
        if (machine == null || stock.isEmpty())
            return;
        ToolTableShaping.Option option = optionAt(level, index);
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
    private void complete(ServerPlayer server, ToolTableShaping.Option option, ToolTableShaping.Stock stock,
                          ForgeRhythm.Result result, ForgeRhythm.Work work, int hammerPoints) {
        Level level = server.level();
        Optional<ItemStack> part = ToolTableShaping.part(option, stock, result.flaw(), work.ratio(), built(level, option, work.ratio(), result.flaw()), level);
        if (part.isEmpty())
            return;
        spend(ToolTableBlockEntity.TOOL, stock, server);
        ItemStack made = ToolTableShaping.finished(option, stock, part.get(), ClientMaterials.on(level), level);
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
            table.setItem(ToolTableBlockEntity.TOOL, main);
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

    private ToolTableShaping.Option optionAt(Level level, int index) {
        return ToolTableShaping.options(shapes(level)).get(index);
    }

    /** The stock, if shape {@code index} is a forged one and there is a hammer and exactly its stock. */
    private Optional<ToolTableShaping.Stock> forgeable(Level level, int index) {
        List<ToolTableShaping.Option> options = ToolTableShaping.options(shapes(level));
        if (mode.get() != MODE_PART || !hasHammer() || index < 0 || index >= options.size() || !options.get(index).forged())
            return Optional.empty();
        return ToolTableShaping.check(station, options.get(index), stock(), ClientMaterials.on(level), level, index == picked() ? aimScale(options.get(index)) : 1f);
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
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player,
                gun ? io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE.get()
                        : FModular.PREP_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != output && super.canTakeItemForPickAll(stack, slot);
    }

    /** Stock of one item at slightly different heats is blended to one heat when it meets in the stock slot, so it stacks. */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slotId >= STOCK && slotId < STOCK + ToolTableBlockEntity.STOCK_SLOTS)
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
            ItemHeat.blend(held, table.getItem(ToolTableBlockEntity.STOCK), player.level());
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
            // From the inventory: a tool to the hammer slot, a blueprint or seasoning to the plan slot, a round to the round slot, else the centre, then the stock slot.
            if (blueprintTab() && held.is(io.github.soundgoodizerfan.feedback.registry.FTags.BLUEPRINT_PAPER))
                return moveItemStackTo(held, EXTRA, EXTRA + 1, false) ? moved : ItemStack.EMPTY;
            boolean plan = dish ? isSeasoning(player.level(), held) : isPlanItem(held);
            boolean round = gun && held.is(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE_ITEM.get());
            if (trim() ? !moveItemStackTo(held, EXTRA, EXTRA + 3, false)
                    : !(plan && moveItemStackTo(held, PLAN, PLAN + 1, false)) && !(round && moveItemStackTo(held, inv() - 1, inv(), false))
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
    public void printBlueprint(ModularDesign design, List<ModularDesign> variants, Player player) {
        if (!blueprintTab())
            return;
        ModularType drawn = ModularTypes.get().types().get(design.type());
        if (drawn == null || drawn.family() != ModularType.Family.DISH) {
            BlueprintTableMenu.send(player, Component.translatable("message.feedback.blueprint_table.cannot_print"));
            return;
        }
        if (BlueprintTableMenu.write(paper, design, variants, player))
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
