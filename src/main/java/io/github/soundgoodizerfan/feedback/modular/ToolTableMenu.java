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
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
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
            TOOL = STOCK + ToolTableBlockEntity.STOCK_SLOTS, PLAN = TOOL + 1, INV = PLAN + 1;
    public static final int MODE_PART = 0, MODE_ASSEMBLE = 1, MODE_REWORK = 2;
    /** Button ids: a mode, taking the tool apart, and {@code PICK + n} choosing shape n. */
    public static final int BTN_MODE = 0, BTN_APART = 3, BTN_PICK = 100;

    public static final int W = 260, H = 282, INV_X = 49, INV_Y = 198;
    public static final int HAMMER_X = 224, HAMMER_Y = 76, OUT_X = 224, OUT_Y = 112;
    public static final int GRID_X = 136, GRID_Y = 76, TOOL_X = 136, TOOL_Y = 44, PLAN_X = 136, PLAN_Y = 142;
    /** The assembly slots, grouped as they are labelled: two heads, a rod, three misc. */
    public static final int ASM_ROW_A = 78, ASM_ROW_B = 114;

    public static int asmX(int i) {
        return i < 2 ? GRID_X + i * 18 : i == 2 ? GRID_X + 46 : GRID_X + (i - 3) * 18;
    }

    public static int asmY(int i) {
        return i < 3 ? ASM_ROW_A : ASM_ROW_B;
    }

    private final Container table;
    private final SimpleContainer output = new SimpleContainer(1);
    private final BlockPos pos;
    private final Player player;
    private final DataSlot mode = DataSlot.standalone();
    private final DataSlot pick = DataSlot.standalone();
    /** The shape being forged, or -1: set when the player begins a pattern, so a finish names the same one. */
    private int forging = -1;

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public ToolTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(ToolTableBlockEntity.SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public ToolTableMenu(int id, Inventory inventory, ToolTableBlockEntity table) {
        this(id, inventory, table, table.getBlockPos());
    }

    private ToolTableMenu(int id, Inventory inventory, Container table, BlockPos pos) {
        super(FMenus.TOOL_TABLE.get(), id);
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
        addSlot(new TableSlot(ToolTableBlockEntity.HAMMER, HAMMER_X, HAMMER_Y, () -> -1, 1, ToolTableBlockEntity::isHammer));
        for (int i = 0; i < ToolTableAssembly.SLOTS; i++)
            addSlot(new TableSlot(ToolTableBlockEntity.ASSEMBLY + i, asmX(i), asmY(i),
                    () -> MODE_ASSEMBLE, ToolTableAssembly.CAPACITY[i], s -> true));
        for (int i = 0; i < ToolTableBlockEntity.STOCK_SLOTS; i++)
            addSlot(new TableSlot(ToolTableBlockEntity.STOCK + i, GRID_X + i % 3 * 18, GRID_Y + i / 3 * 18,
                    () -> -2, 1, s -> true));
        addSlot(new TableSlot(ToolTableBlockEntity.TOOL, TOOL_X, TOOL_Y, () -> MODE_REWORK, 1, s -> ModularStacks.design(s) != null));
        addSlot(new TableSlot(ToolTableBlockEntity.PLAN, PLAN_X, PLAN_Y, () -> MODE_ASSEMBLE, 1, s -> s.is(FModular.BLUEPRINT.get())));
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        addDataSlot(mode);
        addDataSlot(pick);
        if (!player.level().isClientSide)
            output.setItem(0, preview(player.level()));
    }

    /** A slot of the table's own container that is there in some modes only. */
    private class TableSlot extends Slot {
        private final IntSupplier shown;
        private final int cap;
        private final Predicate<ItemStack> takes;

        /** {@code shown} is the mode it shows in; -1 always, -2 in the part and rework modes (the stock cells). */
        TableSlot(int index, int x, int y, IntSupplier shown, int cap, Predicate<ItemStack> takes) {
            super(table, index, x, y);
            this.shown = shown;
            this.cap = cap;
            this.takes = takes;
        }

        @Override
        public boolean isActive() {
            int m = shown.getAsInt();
            return m == -1 || (m == -2 ? mode.get() != MODE_ASSEMBLE : mode.get() == m);
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
        return range(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
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

    /** Whether there is a hand hammer to work with. */
    public boolean hasHammer() {
        return ToolTableBlockEntity.isHammer(hammer());
    }

    /** The shape chosen, if the choice is on the list. */
    public Optional<ToolTableShaping.Option> option(Level level) {
        List<ToolTableShaping.Option> options = ToolTableShaping.options(ModularTypes.on(level));
        return pick.get() >= 0 && pick.get() < options.size() ? Optional.of(options.get(pick.get())) : Optional.empty();
    }

    // --- what the contents make ----------------------------------------------------------------

    /** What taking the output would give, from the contents and the mode: empty if no hammer or nothing valid. */
    public ItemStack preview(Level level) {
        if (!hasHammer())
            return ItemStack.EMPTY;
        Map<ResourceLocation, Material> materials = ClientMaterials.on(level);
        Map<ResourceLocation, ModularType> types = ModularTypes.on(level);
        if (materials.isEmpty() || types.isEmpty())
            return ItemStack.EMPTY;
        return switch (mode.get()) {
            case MODE_PART -> option(level).map(o -> ToolTableShaping.make(o, stock(), materials, level)).orElse(ItemStack.EMPTY);
            case MODE_ASSEMBLE -> ToolTableAssembly.infer(assembly(), plan(), materials, types)
                    .map(ToolTableAssembly.Match::tool).orElse(ItemStack.EMPTY);
            default -> ModularRework.rework(tool(), filled(stock()), materials, types).orElse(ItemStack.EMPTY);
        };
    }

    /** Recompute the output and tell the player's client, as a crafting grid does. */
    private void refresh(boolean send) {
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
            case MODE_ASSEMBLE -> clear(ToolTableBlockEntity.ASSEMBLY, ToolTableAssembly.SLOTS);
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
        hammer().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        table.setChanged();
    }

    // --- buttons: mode, shape, take apart --------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= BTN_PICK) {
            pick.set(id - BTN_PICK);
            forging = -1;
        } else if (id >= BTN_MODE && id < BTN_MODE + 3) {
            mode.set(id - BTN_MODE);
            forging = -1;
        } else if (id == BTN_APART && player instanceof ServerPlayer server) {
            apart(server);
        } else
            return false;
        if (!player.level().isClientSide)
            refresh(true);
        return true;
    }

    /** Take the tool in the tool slot apart: its parts come back to the player, and it costs a point of the hammer. */
    private void apart(ServerPlayer server) {
        ItemStack tool = tool();
        Level level = server.level();
        if (mode.get() != MODE_REWORK || !hasHammer() || ModularStacks.design(tool) == null)
            return;
        List<ItemStack> parts = ModularRework.dismantle(tool, ClientMaterials.on(level), ModularTypes.on(level));
        table.setItem(ToolTableBlockEntity.TOOL, ItemStack.EMPTY);
        parts.forEach(p -> server.getInventory().placeItemBackInInventory(p));
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.4f);
        wearHammer();
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
        ForgeRhythm.Result result = ForgeRhythm.judge(option.variant().rhythm(), step, hits,
                ForgeRhythm.live(material, session.temperature()), ForgeRhythm.noiseFor(hits));
        Optional<ItemStack> part = ToolTableShaping.part(option, stock.get(), result.flaw(), level);
        if (part.isEmpty())
            return;
        clear(ToolTableBlockEntity.STOCK, ToolTableBlockEntity.STOCK_SLOTS);
        server.getInventory().placeItemBackInInventory(part.get());
        wearHammer();
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
        io.github.soundgoodizerfan.feedback.client.HudReadouts.send(server, Component.translatable("forge.feedback.result." + result.flaw(), part.get().getHoverName()), 80);
        refresh(true);
    }

    private ToolTableShaping.Option optionAt(Level level, int index) {
        return ToolTableShaping.options(ModularTypes.on(level)).get(index);
    }

    /** The stock, if shape {@code index} is a forged one and there is a hammer and exactly its stock. */
    private Optional<ToolTableShaping.Stock> forgeable(Level level, int index) {
        List<ToolTableShaping.Option> options = ToolTableShaping.options(ModularTypes.on(level));
        if (mode.get() != MODE_PART || !hasHammer() || index < 0 || index >= options.size() || !options.get(index).forged())
            return Optional.empty();
        return ToolTableShaping.check(options.get(index), stock(), ClientMaterials.on(level), level);
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
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, FModular.TOOL_TABLE.get());
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
            if (!moveItemStackTo(held, INV, slots.size(), true))
                return ItemStack.EMPTY;
            slot.onQuickCraft(held, moved);
        } else if (index < INV) {
            if (!moveItemStackTo(held, INV, slots.size(), false))
                return ItemStack.EMPTY;
        } else if (ToolTableBlockEntity.isHammer(held)) {
            if (!moveItemStackTo(held, HAMMER, HAMMER + 1, false))
                return ItemStack.EMPTY;
        } else if (mode.get() == MODE_ASSEMBLE) {
            if (!(held.is(FModular.BLUEPRINT.get()) && moveItemStackTo(held, PLAN, PLAN + 1, false))
                    && !moveItemStackTo(held, ASM, ASM + ToolTableAssembly.SLOTS, false))
                return ItemStack.EMPTY;
        } else if (mode.get() == MODE_REWORK && ModularStacks.design(held) != null && !slots.get(TOOL).hasItem()) {
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
