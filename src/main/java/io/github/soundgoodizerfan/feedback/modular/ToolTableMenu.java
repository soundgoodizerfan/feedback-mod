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
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * The Tool Table's menu: three jobs on one bench, told apart by a mode the screen's tabs set.
 *
 * <ul>
 *   <li><b>Parts</b> -- pick a shape from the list ({@link ToolTableShaping}), put the stock in the
 *       cells, take the part. A forged shape is played in the screen instead of taken
 *       ({@link #beginForge}, {@link #finishForge}).</li>
 *   <li><b>Assemble</b> -- put parts and stock in the six slots; the tool they make appears
 *       ({@link ToolTableAssembly}), and the parts imply which tool, never a choice.</li>
 *   <li><b>Rework</b> -- a finished tool in the tool slot, and stock to mend it, swap a part or add
 *       a tip or binding ({@link ModularRework}); or take it apart.</li>
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
    /** The third tab: Rework at the other tables, Prepare (the FoodCut minigame's place, not built) at the Prep Table. */
    public static final int MODE_PART = 0, MODE_ASSEMBLE = 1, MODE_REWORK = 2, MODE_PREPARE = MODE_REWORK;
    /** Button ids: a mode, taking the tool apart, a Mechanical Hammer playing the pattern, {@code TYPE + n} choosing dish type n, and {@code PICK + n} choosing shape n. */
    public static final int BTN_MODE = 0, BTN_APART = 3, BTN_MACHINE = 4, BTN_TYPE = 50, BTN_PICK = 100;

    public static final int W = 260, H = 282, INV_X = 49, INV_Y = 198;
    public static final int HAMMER_X = 224, HAMMER_Y = 76, OUT_X = 224, OUT_Y = 112;
    public static final int GRID_X = 136, GRID_Y = 76, TOOL_X = 136, TOOL_Y = 44, PLAN_X = 136, PLAN_Y = 142;
    /** At the Prep Table the plan slot is the seasoning slot of the Prepare tab, beside the work slot. */
    public static final int SEASON_X = 176, SEASON_Y = 44;
    /** The assembly slots, grouped as they are labelled: six alike (the screen labels them by the role their part fills). */
    public static final int ASM_ROW_A = 78, ASM_ROW_B = 114;

    public static int asmX(int i) {
        return GRID_X + i % 3 * 18;
    }

    public static int asmY(int i) {
        return i < 3 ? ASM_ROW_A : ASM_ROW_B;
    }

    /** The Gun Table's assembly slots are a 4 by 4 grid (sixteen parts is the most a gun has), and its blueprint sits below it. */
    public static int gunAsmX(int i) {
        return GRID_X + i % 4 * 18;
    }

    public static int gunAsmY(int i) {
        return ASM_ROW_A + i / 4 * 18;
    }

    public static final int GUN_PLAN_Y = 158, GUN_ROUND_X = GRID_X + 60;

    private final Container table;
    private final SimpleContainer output = new SimpleContainer(1);
    private final BlockPos pos;
    private final Player player;
    private final DataSlot mode = DataSlot.standalone();
    private final DataSlot pick = DataSlot.standalone();
    /** At the Prep Table: which dish type the Assemble tab builds, an index into {@link #dishTypes}. */
    private final DataSlot dishType = DataSlot.standalone();
    /** Which table this is: the Tool Table's menu with a type filter, and at the Gun Table a slot for a round ({@link #gunTable}), at the Prep Table a knife for the hammer. */
    private final ToolTableAssembly.Kind kind;
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

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public ToolTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(FMenus.TOOL_TABLE.get(), ToolTableAssembly.Kind.TOOL, id, inventory, new SimpleContainer(ToolTableBlockEntity.SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public ToolTableMenu(int id, Inventory inventory, ToolTableBlockEntity table) {
        this(FMenus.TOOL_TABLE.get(), ToolTableAssembly.Kind.TOOL, id, inventory, table, table.getBlockPos());
    }

    /** Any of the tables: the Gun Table's and the Prep Table's menus are this class with their {@code kind}. */
    protected ToolTableMenu(net.minecraft.world.inventory.MenuType<?> type, ToolTableAssembly.Kind kind, int id, Inventory inventory, Container table, BlockPos pos) {
        super(type, id);
        this.kind = kind;
        this.gun = kind == ToolTableAssembly.Kind.GUN;
        this.dish = kind == ToolTableAssembly.Kind.DISH;
        if (dish)
            mode.set(MODE_PART);
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
            public void onTake(Player p, ItemStack stack) {
                taken();
            }
        });
        addSlot(new TableSlot(ToolTableBlockEntity.HAMMER, HAMMER_X, HAMMER_Y, () -> -1, 1, this::workTool));
        for (int i = 0; i < ToolTableAssembly.SLOTS; i++)
            addSlot(new TableSlot(ToolTableBlockEntity.ASSEMBLY + i, gun ? gunAsmX(i) : asmX(i), gun ? gunAsmY(i) : asmY(i),
                    () -> MODE_ASSEMBLE, gun ? 2 : dish ? 1 : ToolTableAssembly.CAPACITY, s -> true));
        for (int i = 0; i < ToolTableBlockEntity.STOCK_SLOTS; i++)
            addSlot(new TableSlot(ToolTableBlockEntity.STOCK + i, GRID_X + i % 3 * 18, GRID_Y + i / 3 * 18,
                    () -> dish ? MODE_PART : -2, 1, s -> true));
        // At the Prep Table the tool slot is the work slot: the one food a cut or a pour is done to.
        addSlot(new TableSlot(ToolTableBlockEntity.TOOL, TOOL_X, TOOL_Y, () -> MODE_REWORK, 1,
                s -> dish ? isDish(s) || (Cooking.find(player.level(), s).isPresent() && !isSeasoning(player.level(), s))
                        : ModularStacks.design(s) != null && ToolTableAssembly.kindOf(s) == kind));
        // The Prep Table has no blueprints: its plan slot holds the seasoning a Finish pours onto the dish.
        addSlot(new TableSlot(ToolTableBlockEntity.PLAN, dish ? SEASON_X : PLAN_X, dish ? SEASON_Y : gun ? GUN_PLAN_Y : PLAN_Y,
                () -> dish ? MODE_PREPARE : MODE_ASSEMBLE, 1, s -> dish ? isSeasoning(player.level(), s) : s.is(FModular.BLUEPRINT.get())));
        if (gun) {
            // The rest of a gun's sixteen part slots, and the round its spec sheet is read against (not spent).
            for (int i = ToolTableAssembly.SLOTS; i < ToolTableAssembly.GUN_SLOTS; i++)
                addSlot(new TableSlot(ToolTableBlockEntity.EXTRA + i - ToolTableAssembly.SLOTS, gunAsmX(i), gunAsmY(i),
                        () -> MODE_ASSEMBLE, 1, s -> true));
            addSlot(new TableSlot(ToolTableBlockEntity.ROUND, GUN_ROUND_X, GUN_PLAN_Y, () -> MODE_ASSEMBLE, 1,
                    s -> s.is(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE_ITEM.get())));
        }
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        addDataSlot(mode);
        addDataSlot(pick);
        addDataSlot(dishType);
        if (!player.level().isClientSide)
            output.setItem(0, preview(player.level()));
    }

    /** A slot of the table's own container that is there in some modes only. */
    private class TableSlot extends Slot {
        private final IntSupplier shown;
        private final int cap;
        private final Predicate<ItemStack> takes;

        /** {@code shown} is the mode it shows in; -1 always, -2 in the part and rework modes (the stock cells), -3 never (a Prep Table has no blueprint). */
        TableSlot(int index, int x, int y, IntSupplier shown, int cap, Predicate<ItemStack> takes) {
            super(table, index, x, y);
            this.shown = shown;
            this.cap = cap;
            this.takes = takes;
        }

        @Override
        public boolean isActive() {
            int m = shown.getAsInt();
            return m == -1 || (m == -2 ? mode.get() != MODE_ASSEMBLE : m != -3 && mode.get() == m);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && takes.test(stack);
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

    public int mode() {
        return mode.get();
    }

    /** Which shape of the list is chosen, as an index into {@link ToolTableShaping#options}. */
    public int picked() {
        return pick.get();
    }

    public ItemStack hammer() {
        return table.getItem(ToolTableBlockEntity.HAMMER);
    }

    public ItemStack tool() {
        return table.getItem(ToolTableBlockEntity.TOOL);
    }

    public ItemStack plan() {
        return table.getItem(ToolTableBlockEntity.PLAN);
    }

    public List<ItemStack> assembly() {
        List<ItemStack> out = range(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
        if (gun)
            out.addAll(range(ToolTableBlockEntity.EXTRA, ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS));
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
        return EXTRA + (gun ? ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS + 1 : 0);
    }

    /** Whether this is the Prep Table. */
    public boolean dishTable() {
        return dish;
    }

    /** Whether {@code stack} is what the tool slot takes: a knife at the Prep Table, a hand hammer elsewhere. */
    public boolean workTool(ItemStack stack) {
        return dish ? ToolTableBlockEntity.isKnife(stack) : ToolTableBlockEntity.isHammer(stack);
    }

    /** The types this table makes: guns at the Gun Table, dishes at the Prep Table, everything else at the Tool Table. */
    public Map<ResourceLocation, ModularType> types(Level level) {
        return ToolTableAssembly.ofKind(ModularTypes.on(level), kind);
    }

    /** The types whose parts the Parts list shapes: this table's own (the dish's containers are shaped at the Prep Table). */
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

    /** What the Assemble tab infers from: the picked dish type alone at the Prep Table, else every type of the table. */
    public Map<ResourceLocation, ModularType> assemblyTypes(Level level) {
        Map<ResourceLocation, ModularType> all = types(level);
        if (!dish || all.isEmpty())
            return all;
        ResourceLocation id = dishTypes(level).get(typePicked(level));
        return Map.of(id, all.get(id));
    }

    public List<ItemStack> stock() {
        return range(ToolTableBlockEntity.STOCK, ToolTableBlockEntity.STOCK_SLOTS);
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
        return workTool(hammer());
    }

    /** The shape chosen, if the choice is on the list. */
    public Optional<ToolTableShaping.Option> option(Level level) {
        List<ToolTableShaping.Option> options = ToolTableShaping.options(shapes(level));
        return pick.get() >= 0 && pick.get() < options.size() ? Optional.of(options.get(pick.get())) : Optional.empty();
    }

    // --- what the contents make ----------------------------------------------------------------

    /** What taking the output would give, from the contents and the mode: empty if no hammer or nothing valid. */
    public ItemStack preview(Level level) {
        if (!hasHammer())
            return ItemStack.EMPTY;
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = types(level);
        if (materials.isEmpty() || types.isEmpty())
            return ItemStack.EMPTY;
        return switch (mode.get()) {
            case MODE_PART -> option(level).map(o -> ToolTableShaping.make(o, stock(), materials, level)).orElse(ItemStack.EMPTY);
            case MODE_ASSEMBLE -> ToolTableAssembly.infer(assembly(), plan(), materials, assemblyTypes(level))
                    .map(ToolTableAssembly.Match::tool).orElse(ItemStack.EMPTY);
            default -> dish ? ItemStack.EMPTY : ModularRework.rework(tool(), filled(stock()), materials, types).orElse(ItemStack.EMPTY);
        };
    }

    /** Recompute the output and tell the player's client, as a crafting grid does. */
    private void refresh(boolean send) {
        seen = ItemStack.hashStackList(range(0, table.getContainerSize()));
        ItemStack out = preview(player.level());
        output.setItem(0, out);
        if (send && player instanceof ServerPlayer server) {
            setRemoteSlot(OUT, out);
            server.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), OUT, out));
        }
    }

    /** The output was taken: spend what made it, and a point of the hammer. */
    private void taken() {
        if (player.level().isClientSide)
            return;
        switch (mode.get()) {
            case MODE_PART -> clear(ToolTableBlockEntity.STOCK, ToolTableBlockEntity.STOCK_SLOTS);
            case MODE_ASSEMBLE -> {
                clear(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
                if (gun)
                    clear(ToolTableBlockEntity.EXTRA, ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS);
            }
            default -> {
                clear(ToolTableBlockEntity.STOCK, ToolTableBlockEntity.STOCK_SLOTS);
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
        hammer().hurtAndBreak(points, player, EquipmentSlot.MAINHAND);
        table.setChanged();
    }

    // --- buttons: mode, shape, take apart --------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        // No hammer plays a pattern for a container.
        if (dish && id == BTN_MACHINE)
            return false;
        if (id >= BTN_TYPE && id < BTN_PICK) {
            if (!dish || id - BTN_TYPE >= dishTypes(player.level()).size())
                return false;
            dishType.set(id - BTN_TYPE);
        } else if (id >= BTN_PICK) {
            pick.set(id - BTN_PICK);
            forging = -1;
        } else if (id >= BTN_MODE && id < BTN_MODE + 3) {
            mode.set(id - BTN_MODE);
            forging = -1;
        } else if (id == BTN_APART && player instanceof ServerPlayer server) {
            apart(server);
        } else if (id == BTN_MACHINE && player instanceof ServerPlayer server) {
            machineForge(server);
        } else
            return false;
        if (!player.level().isClientSide)
            refresh(true);
        return true;
    }

    /**
     * Take the tool in the tool slot apart: its parts come back to the player, and it costs a point of the hammer. At the Prep
     * Table it is a finished dish in the work slot and a point of the knife; a soup is refused and the finish is lost.
     */
    private void apart(ServerPlayer server) {
        ItemStack tool = tool();
        Level level = server.level();
        if (mode.get() != MODE_REWORK || !hasHammer() || ModularStacks.design(tool) == null || (dish && !isDish(tool)))
            return;
        List<ItemStack> parts;
        if (dish) {
            Cooking.settle(tool, level, io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable.at(level).value());
            Optional<List<ItemStack>> out = ModularRework.dismantleDish(tool, ClientMaterials.on(level), types(level));
            if (out.isEmpty()) {
                io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("prep.feedback.apart.soup"), 60);
                return;
            }
            parts = out.get();
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("prep.feedback.apart"), 60);
        } else
            parts = ModularRework.dismantle(tool, ClientMaterials.on(level), types(level));
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
        if (!dish || mode.get() != MODE_PREPARE || work().isEmpty())
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
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("prep.feedback.refused"), 60);
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
            int[] landed = marks.stream().mapToInt(m -> PrepWork.land(m, entry.food())).toArray();
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
                    || type.afterRole() == null || heldTicks <= 0 || heldTicks > FTuning.POUR_MAX_TICKS || heldTicks > elapsed)
                return false;
            float strength = PrepWork.strength(PrepWork.poured(heldTicks, PrepWork.flow(what.get().material())));
            // The seasoning joins the design as its finish part, carrying its state and any cut, plus how it was poured.
            ModularDesign.Part base = ToolTableAssembly.part(type.afterRole(), what.get(), seasoning, 1);
            java.util.Map<String, Float> dims = new java.util.HashMap<>(base.dims());
            dims.put("pour", strength);
            List<ModularDesign.Part> parts = new ArrayList<>(design.parts());
            parts.add(new ModularDesign.Part(base.role(), base.material(), base.form(), base.count(), base.kind(), base.variant(), base.flaw(), dims));
            ItemStack done = work().copy();
            if (!ModularStacks.assemble(done, new ModularDesign(design.type(), parts, design.edge()), materials, ModularTypes.on(level)))
                return false;
            table.setItem(ToolTableBlockEntity.PLAN, ItemStack.EMPTY);
            table.setItem(ToolTableBlockEntity.TOOL, done);
            said = pourName(strength);
        }
        io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, said, 60);
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
        if (!ForgeRhythm.workable(material, stock.get().coldest())) {
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("forge.feedback.unworkable"), 60);
            return;
        }
        if (!ForgeRhythm.movable(optionAt(level, index).variant().rhythm(), strikes(), material)) {
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("forge.feedback.unmovable"), 60);
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
        int[] live = ForgeRhythm.live(material, session.temperature());
        net.minecraft.util.RandomSource noise = ForgeRhythm.noiseFor(hits);
        ForgeRhythm.Result result = ForgeRhythm.judge(option.variant().rhythm(), step, hits, live, noise);
        ForgeRhythm.Work work = ForgeRhythm.work(option.variant().rhythm(), strikes(), material, hits, live, noise);
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
        if (!ForgeRhythm.workable(material, stock.get().coldest())) {
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("forge.feedback.unworkable"), 60);
            return;
        }
        if (!ForgeRhythm.movable(rhythm, strikes, material)) {
            io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("forge.feedback.unmovable"), 60);
            return;
        }
        int step = ForgeRhythm.step(material);
        List<ForgeRhythm.Hit> hits = new java.util.ArrayList<>();
        for (int i = 0; i < rhythm.length(); i++)
            if (ForgeRhythm.note(rhythm.charAt(i)))
                hits.add(new ForgeRhythm.Hit(ForgeRhythm.noteTick(i, step), rhythm.charAt(i) == 'h'));
        int[] live = ForgeRhythm.live(material, stock.get().coldest());
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
        Optional<ItemStack> part = ToolTableShaping.part(option, stock, result.flaw(), work.ratio(), level);
        if (part.isEmpty())
            return;
        clear(ToolTableBlockEntity.STOCK, ToolTableBlockEntity.STOCK_SLOTS);
        server.getInventory().placeItemBackInInventory(part.get());
        if (hammerPoints > 0)
            wearHammer(hammerPoints);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
        Component said = Component.translatable("forge.feedback.result." + result.flaw(), part.get().getHoverName());
        if (work.wasted() > 0)
            said = said.copy().append(" ").append(Component.translatable("forge.feedback.wasted", work.wasted()));
        io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, said, 80);
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
        return ToolTableShaping.check(options.get(index), stock(), ClientMaterials.on(level), level);
    }

    /**
     * Another player at the same table changes a slot through their own menu, so this one is never
     * told: each tick, if the contents are not what the output was made from, make it again.
     */
    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide) {
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
        ForgeRhythm.end(player.getUUID());
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player,
                gun ? io.github.soundgoodizerfan.feedback.registry.FWeapons.GUN_TABLE.get()
                        : dish ? FModular.PREP_TABLE.get() : FModular.TOOL_TABLE.get());
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != output && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
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
        } else if (dish && mode.get() == MODE_PREPARE) {
            int into = isSeasoning(player.level(), held) ? PLAN : TOOL;
            if (!moveItemStackTo(held, into, into + 1, false))
                return ItemStack.EMPTY;
        } else if (mode.get() == MODE_ASSEMBLE) {
            if (!(held.is(FModular.BLUEPRINT.get()) && moveItemStackTo(held, PLAN, PLAN + 1, false))
                    && !(gun && held.is(io.github.soundgoodizerfan.feedback.registry.FWeapons.CARTRIDGE_ITEM.get())
                    && moveItemStackTo(held, inv() - 1, inv(), false))
                    && !moveItemStackTo(held, ASM, ASM + ToolTableAssembly.SLOTS, false)
                    && !(gun && moveItemStackTo(held, EXTRA, EXTRA + ToolTableAssembly.GUN_SLOTS - ToolTableAssembly.SLOTS, false)))
                return ItemStack.EMPTY;
        } else if (mode.get() == MODE_REWORK && ModularStacks.design(held) != null && ToolTableAssembly.kindOf(held) == kind && !slots.get(TOOL).hasItem()) {
            if (!moveItemStackTo(held, TOOL, TOOL + 1, false))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(held, STOCK, STOCK + ToolTableBlockEntity.STOCK_SLOTS, false))
            return ItemStack.EMPTY;
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

    /** The coldest piece of stock in the cells, as this side sees it: what the screen glows by. */
    public float heat(Level level) {
        float coldest = Float.MAX_VALUE;
        for (ItemStack s : stock())
            if (!s.isEmpty())
                coldest = Math.min(coldest, ItemHeat.get(s, level).value());
        return coldest == Float.MAX_VALUE ? 0 : coldest;
    }
}
