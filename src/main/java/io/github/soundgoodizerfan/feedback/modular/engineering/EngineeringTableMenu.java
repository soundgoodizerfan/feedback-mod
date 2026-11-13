// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import io.github.soundgoodizerfan.feedback.modular.blueprint.BlueprintBookItem;
import io.github.soundgoodizerfan.feedback.modular.bench.MenuTabs;
import io.github.soundgoodizerfan.feedback.modular.item.ModularRework;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.treatise.School;
import io.github.soundgoodizerfan.feedback.modular.bench.ModularAssembly;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchMenu;
import io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseBinderItem;
import io.github.soundgoodizerfan.feedback.modular.treatise.TreatiseItem;
import io.github.soundgoodizerfan.feedback.modular.treatise.Treatises;
import io.github.soundgoodizerfan.feedback.net.MinigameOpenPayload;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.registry.FEngineering;
import io.github.soundgoodizerfan.feedback.registry.FMenus;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Engineering Table's menu. Four tabs ({@link #TAB_CRAFT} and the rest), chosen by the server through {@link MenuTabs}; each tab's
 * slots are inert while it is not open ({@code isActive} false, {@code mayPlace} false), so quick-move fills only the open tab. Every slot
 * but the output is a slot of the block entity and keeps its contents when the screen closes.
 *
 * <p>The Crafting tab recomputes its output on the server whenever a table slot changes, a tab is chosen or the menu opens
 * ({@link EngineeringAssembly#plan}); {@link #status} is synced so the screen can say why nothing is offered. Fit, Repair and Treatise
 * tabs hold their slots here and act through buttons ({@link #BTN_FIT_APPLY} and the rest) decided by {@link EngineeringRework}; {@link #note}
 * is synced so the screen can say what the last action did or why it refused.
 *
 * <p>Menu slot index: {@link #OUT} 0; table container slot {@code c} is menu slot {@link #slot(int) 1 + c}; {@link #BINDER_TREATISE}
 * is the binder again on the Treatise tab; the player's inventory follows from {@link #INV} (27 main, then 9 hotbar).
 */
public class EngineeringTableMenu extends AbstractContainerMenu {

    public static final int TAB_CRAFT = 0, TAB_FIT = 1, TAB_REPAIR = 2, TAB_TREATISE = 3, TABS = 4;
    /** Menu buttons {@code BTN_TAB + tab} select a tab. */
    public static final int BTN_TAB = 0;
    /** Fit tab: fit every attachment. Repair tab: repair, swap a part, take apart. Treatise tab: take the specimen apart onto the blank. */
    public static final int BTN_FIT_APPLY = 100, BTN_REPAIR = 200, BTN_SWAP = 201, BTN_APART = 202, BTN_LEARN = 300;

    public static final int OUT = 0;
    /** The binder's second slot, shown on the Treatise tab (same container slot as the Crafting tab's). */
    public static final int BINDER_TREATISE = 1 + EngineeringTableBlockEntity.SIZE;
    public static final int INV = BINDER_TREATISE + 1;

    /** The panel's size; slot positions are pixels in it, and the screen draws a socket at each live slot. */
    public static final int W = 216, H = 210;
    /** The width of the popout beside the panel that holds the hand tools and the fixings. */
    public static final int POP_W = 62;
    /**
     * The layout. Crafting: grid top-left, arrow, output at the right; the blueprint bottom-left, the binder bottom-right. The four hand
     * tools and three fasteners sit in a popout column beside the panel, on every tab ({@link #POP_W}). Fit: the item, then the 4x4 attachment grid. Repair: the item, then the part-stock grid.
     * Treatise: binder, blank treatise, specimen in a row.
     */
    public static final int OUT_X = 146, OUT_Y = 37, GRID_X = 30, GRID_Y = 18, BLUEPRINT_X = 8, BLUEPRINT_Y = 92, TOOL_X = W + 12, TOOL_Y = 30,
            FASTENER_X = W + 12, FASTENER_Y = 124, BINDER_X = 192, BINDER_Y = 92, FIT_ITEM_X = 30, FIT_ITEM_Y = 36, ATTACH_X = 90, ATTACH_Y = 18,
            REPAIR_ITEM_X = 10, REPAIR_ITEM_Y = 36, STOCK_X = 98, STOCK_Y = 18, BINDER_TREATISE_X = 40, BLANK_X = 88, SPECIMEN_X = 136,
            TREATISE_Y = 44, INV_X = 27, INV_Y = 126;

    private final MenuTabs tabs = new MenuTabs(BTN_TAB, TABS);
    private final DataSlot status = DataSlot.standalone();
    /** The school volume the Crafting tab lacks: {@code (School ordinal + 1) * 8 + volume}, 0 for none (synced). */
    private final DataSlot school = DataSlot.standalone();
    /** What the last Fit, Repair or Treatise action said: an {@link EngineeringRework} note (synced), cleared by choosing a tab. */
    private final DataSlot note = DataSlot.standalone();
    /** The school volume a {@link EngineeringRework#SCHOOL} note names, as {@link #school} codes it (synced). */
    private final DataSlot noteVolume = DataSlot.standalone();
    private final Container table;
    private final Container output = new ResultContainer();
    private final Player player;
    private final BlockPos pos;
    private EngineeringAssembly.Plan plan = EngineeringAssembly.Plan.NONE;
    /** The hand-work game the server asked this player to play (empty for none), when it began (game time) and how many rounds it has. */
    private String gameId = "";
    private long gameStart;
    private int gameRounds;

    /** Client-side open: the position comes in the open packet and the slots start empty. */
    public EngineeringTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, new SimpleContainer(EngineeringTableBlockEntity.SIZE), buf.readBlockPos());
    }

    /** Server-side open. */
    public EngineeringTableMenu(int id, Inventory inventory, EngineeringTableBlockEntity block) {
        this(id, inventory, block, block.getBlockPos());
    }

    private EngineeringTableMenu(int id, Inventory inventory, Container backing, BlockPos pos) {
        super(FMenus.ENGINEERING_TABLE.get(), id);
        this.player = inventory.player;
        this.pos = pos;
        this.table = new PartBenchMenu.Watched(backing, () -> {
            if (!player.level().isClientSide)
                refresh();
        });
        addSlot(new Slot(output, 0, OUT_X, OUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return tabs.get() == TAB_CRAFT;
            }

            @Override
            public boolean mayPickup(Player p) {
                return plan.ok();
            }

            @Override
            public void onTake(Player p, ItemStack stack) {
                taken();
            }
        });
        int c = EngineeringTableBlockEntity.GRID;
        for (int r = 0; r < 3; r++)
            for (int col = 0; col < 3; col++)
                add(c++, GRID_X + col * 18, GRID_Y + r * 18, 1 << TAB_CRAFT, 64, s -> true);
        add(EngineeringTableBlockEntity.BLUEPRINT, BLUEPRINT_X, BLUEPRINT_Y, 1 << TAB_CRAFT, 1, EngineeringTableMenu::isBlueprint);
        for (int i = 0; i < 4; i++)
            add(EngineeringTableBlockEntity.TOOLS + i, TOOL_X, TOOL_Y + i * 18, ALL_TABS, 1, ToolAbilities::isTool);
        for (int i = 0; i < 3; i++)
            add(EngineeringTableBlockEntity.FASTENERS + i, FASTENER_X, FASTENER_Y + i * 18, ALL_TABS, 64, Fasteners::is);
        add(EngineeringTableBlockEntity.BINDER, BINDER_X, BINDER_Y, 1 << TAB_CRAFT, 1, EngineeringTableMenu::isBinder);
        add(EngineeringTableBlockEntity.FIT_ITEM, FIT_ITEM_X, FIT_ITEM_Y, 1 << TAB_FIT, 1, EngineeringTableMenu::isModular);
        for (int i = 0; i < 16; i++)
            add(EngineeringTableBlockEntity.ATTACHMENTS + i, ATTACH_X + (i % 4) * 18, ATTACH_Y + (i / 4) * 18, 1 << TAB_FIT, 64, this::isStock);
        add(EngineeringTableBlockEntity.REPAIR_ITEM, REPAIR_ITEM_X, REPAIR_ITEM_Y, 1 << TAB_REPAIR, 1, EngineeringTableMenu::isModular);
        for (int i = 0; i < 9; i++)
            add(EngineeringTableBlockEntity.REPAIR_STOCK + i, STOCK_X + (i % 3) * 18, STOCK_Y + (i / 3) * 18, 1 << TAB_REPAIR, 64, this::isStock);
        add(EngineeringTableBlockEntity.BLANK, BLANK_X, TREATISE_Y, 1 << TAB_TREATISE, 1,
                s -> s.is(FModular.BLANK_TREATISE.get()) || EngineeringRework.isBlankPaper(s));
        add(EngineeringTableBlockEntity.SPECIMEN, SPECIMEN_X, TREATISE_Y, 1 << TAB_TREATISE, 1, EngineeringTableMenu::isModular);
        add(EngineeringTableBlockEntity.BINDER, BINDER_TREATISE_X, TREATISE_Y, 1 << TAB_TREATISE, 1, EngineeringTableMenu::isBinder);
        for (int row = 0; row < 3; row++)
            for (int column = 0; column < 9; column++)
                addSlot(new Slot(inventory, column + row * 9 + 9, INV_X + column * 18, INV_Y + row * 18));
        for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column, INV_X + column * 18, INV_Y + 58));
        addDataSlot(tabs.slot());
        addDataSlot(status);
        addDataSlot(school);
        addDataSlot(note);
        addDataSlot(noteVolume);
        if (!player.level().isClientSide)
            refresh();
    }

    /** The menu slot of table container slot {@code container} ({@link EngineeringTableBlockEntity}'s constants). */
    public static int slot(int container) {
        return 1 + container;
    }

    private void add(int index, int x, int y, int tabMask, int cap, Predicate<ItemStack> takes) {
        addSlot(new TabSlot(index, x, y, tabMask, cap, takes));
    }

    /** A slot of the table's container, live only on the tabs in {@code tabMask}. */
    private class TabSlot extends Slot {
        private final int tabMask, cap;
        private final Predicate<ItemStack> takes;

        TabSlot(int index, int x, int y, int tabMask, int cap, Predicate<ItemStack> takes) {
            super(table, index, x, y);
            this.tabMask = tabMask;
            this.cap = cap;
            this.takes = takes;
        }

        @Override
        public boolean isActive() {
            return (tabMask >> tabs.get() & 1) != 0;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && takes.test(stack);
        }

        @Override
        public boolean mayPickup(Player p) {
            return isActive();
        }

        @Override
        public int getMaxStackSize() {
            return cap;
        }
    }

    /** Every tab: the tool row is wanted wherever a hand tool is used. */
    private static final int ALL_TABS = (1 << TABS) - 1;

    /** A modular item (the Fit, Repair and specimen slots): one with a design. */
    public static boolean isModular(ItemStack stack) {
        return ModularStacks.design(stack) != null;
    }

    private boolean isStock(ItemStack stack) {
        return EngineeringRework.isStock(stack, player.level());
    }

    public static boolean isBlueprint(ItemStack stack) {
        return stack.is(FModular.BLUEPRINT.get()) || stack.getItem() instanceof BlueprintBookItem;
    }

    public static boolean isBinder(ItemStack stack) {
        return stack.getItem() instanceof TreatiseBinderItem || stack.getItem() instanceof TreatiseItem;
    }

    /** The open tab ({@link #TAB_CRAFT} and the rest). */
    public int tab() {
        return tabs.get();
    }

    /** Why the Crafting tab offers what it does: an {@link EngineeringAssembly} status constant (synced). */
    public int status() {
        return status.get();
    }

    /** The lang key of the treatise volume the Crafting tab lacks (while {@link #status} is the school one), or empty. */
    public String missingVolumeKey() {
        return volumeKey(school.get());
    }

    /** What the last Fit, Repair or Treatise action said ({@link EngineeringRework} notes; 0 for nothing). */
    public int note() {
        return note.get();
    }

    /** The lang key of the volume a {@link EngineeringRework#SCHOOL} note names, or empty. */
    public String noteVolumeKey() {
        return volumeKey(noteVolume.get());
    }

    private static String volumeKey(int v) {
        School[] all = School.values();
        return v < 8 || v / 8 > all.length ? "" : all[v / 8 - 1].itemKey(v % 8);
    }

    private int missingSchool(Level level) {
        EngineeringInput in = input();
        String field = "";
        for (var holder : level.getRecipeManager().getAllRecipesFor(FEngineering.TYPE.get())) {
            var recipe = holder.value();
            if (recipe.attempt(in).refusal() == EngineeringRecipe.Refusal.SCHOOL) {
                field = recipe.school();
                break;
            }
        }
        if (field.isEmpty()) {
            var types = EngineeringAssembly.types(level);
            var found = ModularAssembly.infer(in.grid(), table.getItem(EngineeringTableBlockEntity.BLUEPRINT), ClientMaterials.on(level), types);
            if (found.isPresent() && types.get(found.get().type()) != null) {
                var t = types.get(found.get().type());
                field = t.family() == io.github.soundgoodizerfan.feedback.modular.type.ModularType.Family.HAFTED
                        ? io.github.soundgoodizerfan.feedback.modular.type.ModularFaces.school(ModularStacks.design(found.get().tool()), t) : t.school();
            }
        }
        School s = School.of(field);
        return s == null ? 0 : (s.ordinal() + 1) * 8 + Math.min(School.volumeOf(field), s.volumes());
    }

    public BlockPos pos() {
        return pos;
    }

    private List<ItemStack> range(int first, int n) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < n; i++)
            out.add(table.getItem(first + i));
        return out;
    }

    private EngineeringInput input() {
        return new EngineeringInput(range(EngineeringTableBlockEntity.GRID, 9), range(EngineeringTableBlockEntity.TOOLS, 4),
                range(EngineeringTableBlockEntity.FASTENERS, 3), table.getItem(EngineeringTableBlockEntity.BINDER),
                Treatises.mask(player, ContainerLevelAccess.create(player.level(), pos)),
                Treatises.notes(player, ContainerLevelAccess.create(player.level(), pos), table.getItem(EngineeringTableBlockEntity.BINDER)));
    }

    /** Recomputes the Crafting tab's output (server side). */
    private void refresh() {
        Level level = player.level();
        if (tabs.get() != TAB_CRAFT) {
            plan = EngineeringAssembly.Plan.NONE;
        } else {
            plan = EngineeringAssembly.plan(level, input(), table.getItem(EngineeringTableBlockEntity.BLUEPRINT));
        }
        status.set(plan.status());
        school.set(plan.status() == EngineeringAssembly.SCHOOL ? missingSchool(level) : 0);
        output.setItem(0, plan.ok() ? plan.result().copy() : ItemStack.EMPTY);
        broadcastChanges();
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player p) {
        // Taking an output that has a hand-work game plays the game instead; the craft completes when its result arrives.
        if (slotId == OUT && plan.playsGame() && !p.level().isClientSide && p instanceof ServerPlayer server) {
            if (type == ClickType.PICKUP || type == ClickType.QUICK_MOVE)
                beginMinigame(server);
            return; // every other click type on the output is swallowed
        }
        super.clicked(slotId, button, type, p);
    }

    private void beginMinigame(ServerPlayer server) {
        long now = server.level().getGameTime();
        if (!gameId.isEmpty() && now - gameStart < 20)
            return;
        gameId = plan.minigame();
        gameRounds = plan.rounds();
        gameStart = now;
        PacketDistributor.sendToPlayer(server, new MinigameOpenPayload(containerId, gameId, gameRounds, plan.quality(), plan.data()));
    }

    /**
     * The client's one result for the game this menu asked for: {@code percent} is clamped to 0..100 and scored as nothing when it came faster
     * than an honest play could ({@link EngineeringMinigames#minTicks}); it moves only the start condition, so no score refuses the craft. The
     * craft completes here and the output goes to the player's inventory.
     */
    public void finishMinigame(ServerPlayer server, int percent) {
        if (gameId.isEmpty())
            return;
        String id = gameId;
        int rounds = gameRounds;
        long elapsed = server.level().getGameTime() - gameStart;
        gameId = "";
        float score = elapsed < EngineeringMinigames.minTicks(id, gameRounds) ? 0f : Math.max(0, Math.min(100, percent)) / 100f;
        if (EngineeringMinigames.NOTES.toString().equals(id)) {
            // The learning game: the volume is written with notes as good as the reading, whatever the score (never a gate).
            if (tabs.get() == TAB_TREATISE && EngineeringRework.learnRounds(table, server) == rounds) {
                EngineeringRework.Outcome out = EngineeringRework.learn(table, server, score, true);
                note.set(out.note());
                noteVolume.set(out.volume());
                broadcastChanges();
            }
            return;
        }
        EngineeringAssembly.Plan done = EngineeringAssembly.plan(server.level(), input(), table.getItem(EngineeringTableBlockEntity.BLUEPRINT), score);
        if (tabs.get() != TAB_CRAFT || !done.playsGame() || !id.equals(done.minigame()) || done.rounds() != rounds) {
            refresh();
            return;
        }
        ItemStack out = done.result().copy();
        AssemblyRecording.Snapshot before = AssemblyRecording.snapshot(input());
        done.take().accept(input(), server);
        AssemblyRecording.record(server, server.level(), before, input(), out);
        server.getInventory().placeItemBackInInventory(out);
        table.setChanged();
        refresh();
    }

    /** The output was taken: wear, spend, consume, then offer the next. */
    private void taken() {
        if (player.level().isClientSide || !plan.ok())
            return;
        AssemblyRecording.Snapshot before = AssemblyRecording.snapshot(input());
        ItemStack made = plan.result().copy();
        plan.take().accept(input(), player);
        AssemblyRecording.record(player, player.level(), before, input(), made);
        for (int i = 0; i < table.getContainerSize(); i++)
            if (table.getItem(i).isEmpty() && table.getItem(i) != ItemStack.EMPTY)
                table.setItem(i, ItemStack.EMPTY);
        table.setChanged();
        refresh();
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer server))
            return false;
        if (tabs.click(id)) {
            gameId = "";
            note.set(0);
            noteVolume.set(0);
            refresh();
            return true;
        }
        int tab = tabs.get();
        EngineeringRework.Outcome out = switch (id) {
            case BTN_FIT_APPLY -> tab == TAB_FIT ? EngineeringRework.fit(table, server, heldMask()) : null;
            case BTN_REPAIR -> tab == TAB_REPAIR ? EngineeringRework.repair(table, server, ModularRework.Mode.MEND) : null;
            case BTN_SWAP -> tab == TAB_REPAIR ? EngineeringRework.repair(table, server, ModularRework.Mode.SWAP) : null;
            case BTN_APART -> tab == TAB_REPAIR ? EngineeringRework.takeApart(table, server) : null;
            case BTN_LEARN -> tab == TAB_TREATISE ? learnClicked(server) : null;
            default -> null;
        };
        if (out == null)
            return false;
        note.set(out.note());
        noteVolume.set(out.volume());
        broadcastChanges();
        return true;
    }

    /**
     * Learn: blank paper copies a blueprint at once; a Blank Treatise opens the learning game ({@link EngineeringMinigames#NOTES}) and the
     * volume is written when its result arrives ({@link #finishMinigame}), with notes as good as the play.
     */
    private EngineeringRework.Outcome learnClicked(ServerPlayer server) {
        if (!table.getItem(EngineeringTableBlockEntity.BLANK).is(io.github.soundgoodizerfan.feedback.registry.FModular.BLANK_TREATISE.get()))
            return EngineeringRework.learn(table, server);
        int rounds = EngineeringRework.learnRounds(table, server);
        if (rounds < 0)
            return EngineeringRework.Outcome.of(-rounds);
        long now = server.level().getGameTime();
        if (!gameId.isEmpty() && now - gameStart < 20)
            return EngineeringRework.Outcome.of(0);
        gameId = EngineeringMinigames.NOTES.toString();
        gameRounds = rounds;
        gameStart = now;
        PacketDistributor.sendToPlayer(server, new MinigameOpenPayload(containerId, gameId, gameRounds, 1, ""));
        return EngineeringRework.Outcome.of(0);
    }

    /** The school volumes held where this table looks: the maker's inventory and the shelves beside it, and the binder slot. */
    private int heldMask() {
        return Treatises.mask(player, ContainerLevelAccess.create(player.level(), pos)) | Treatises.mask(table.getItem(EngineeringTableBlockEntity.BINDER));
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;
        ItemStack held = slot.getItem();
        ItemStack moved = held.copy();
        if (index == OUT) {
            if (p.level().isClientSide)
                return ItemStack.EMPTY;
            // As vanilla's crafting: take the output over and over while it is offered and fits.
            ItemStack first = ItemStack.EMPTY;
            for (int n = 0; n < 64 && slot.hasItem() && plan.ok(); n++) {
                ItemStack out = slot.getItem().copy();
                if (!first.isEmpty() && !ItemStack.isSameItemSameComponents(first, out))
                    break;
                if (!moveItemStackTo(out, INV, slots.size(), true))
                    break;
                if (!out.isEmpty())
                    break;
                if (first.isEmpty())
                    first = moved.copy();
                slot.set(ItemStack.EMPTY);
                taken();
            }
            return first;
        }
        if (index < INV) {
            if (!moveItemStackTo(held, INV, slots.size(), true))
                return ItemStack.EMPTY;
        } else {
            // From the inventory: the first slot of the open tab that takes it; each slot's mayPlace says which tab and what.
            int[][] order = {
                    {slot(EngineeringTableBlockEntity.TOOLS), slot(EngineeringTableBlockEntity.TOOLS) + 4},
                    {slot(EngineeringTableBlockEntity.FASTENERS), slot(EngineeringTableBlockEntity.FASTENERS) + 3},
                    {slot(EngineeringTableBlockEntity.BINDER), slot(EngineeringTableBlockEntity.BINDER) + 1},
                    {BINDER_TREATISE, BINDER_TREATISE + 1},
                    {slot(EngineeringTableBlockEntity.BLUEPRINT), slot(EngineeringTableBlockEntity.BLUEPRINT) + 1},
                    {slot(EngineeringTableBlockEntity.BLANK), slot(EngineeringTableBlockEntity.BLANK) + 1},
                    {slot(EngineeringTableBlockEntity.SPECIMEN), slot(EngineeringTableBlockEntity.SPECIMEN) + 1},
                    {slot(EngineeringTableBlockEntity.FIT_ITEM), slot(EngineeringTableBlockEntity.FIT_ITEM) + 1},
                    {slot(EngineeringTableBlockEntity.REPAIR_ITEM), slot(EngineeringTableBlockEntity.REPAIR_ITEM) + 1},
                    {slot(EngineeringTableBlockEntity.ATTACHMENTS), slot(EngineeringTableBlockEntity.ATTACHMENTS) + 16},
                    {slot(EngineeringTableBlockEntity.REPAIR_STOCK), slot(EngineeringTableBlockEntity.REPAIR_STOCK) + 9},
                    {slot(EngineeringTableBlockEntity.GRID), slot(EngineeringTableBlockEntity.GRID) + 9}};
            boolean any = false;
            for (int[] r : order)
                if (!held.isEmpty() && moveItemStackTo(held, r[0], r[1], false))
                    any = true;
            if (!any)
                return ItemStack.EMPTY;
        }
        if (held.isEmpty())
            slot.setByPlayer(ItemStack.EMPTY);
        else
            slot.setChanged();
        if (held.getCount() == moved.getCount())
            return ItemStack.EMPTY;
        slot.onTake(p, held);
        return moved;
    }

    @Override
    public boolean stillValid(Player p) {
        return !(table instanceof PartBenchMenu.Watched) || table.stillValid(p);
    }
}
